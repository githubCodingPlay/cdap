/*
 * Copyright © 2016 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package co.cask.cdap.security.store;

import co.cask.cdap.api.security.store.SecureStore;
import co.cask.cdap.api.security.store.SecureStoreData;
import co.cask.cdap.api.security.store.SecureStoreManager;
import co.cask.cdap.api.security.store.SecureStoreMetadata;
import co.cask.cdap.common.conf.CConfiguration;
import co.cask.cdap.common.conf.Constants;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * File based implementation of secure store. Uses Java JCEKS based keystore.
 *
 * When the client calls a put, the key is put in the keystore. The system then flushes
 * the keystore to the file system.
 * During the flush, the current file is first written to temporary file (_NEW).
 * If that is successful then the temporary file is renamed atomically to the secure store file.
 * If anything fails during this process then the keystore reverts to the last successfully written file.
 * The keystore is flushed to the filesystem after every put and delete.
 *
 * This class is marked as Singleton because it won't work if this class is not a Singleton.
 * Setting in(Scopes.Singleton) in the bindings doesn't work because we are binding this
 * class to two interfaces and we need the instance to be shared between them.
 */
@Singleton
public class FileSecureStore implements SecureStore, SecureStoreManager {
  private static final Logger LOG = LoggerFactory.getLogger(FileSecureStore.class);
  private static final String SCHEME_NAME = "jceks";
  /** Separator between the namespace name and the key name */
  private static final String NAME_SEPARATOR = ":";

  private final char[] password;
  private final Path path;
  private final Lock readLock;
  private final Lock writeLock;
  private final KeyStore keyStore;

  @Inject
  FileSecureStore(CConfiguration cConf) throws IOException {
    // Get the path to the keystore file
    String pathString = cConf.get(Constants.Security.Store.FILE_PATH);
    Path dir = Paths.get(pathString);
    path = dir.resolve(cConf.get(Constants.Security.Store.FILE_NAME));
    // Get the keystore password
    password = cConf.get(Constants.Security.Store.FILE_PASSWORD).toCharArray();

    keyStore = locateKeystore(path, password);
    ReadWriteLock lock = new ReentrantReadWriteLock(true);
    readLock = lock.readLock();
    writeLock = lock.writeLock();
  }

  /**
   * Stores an element in the secure store.
   * @param namespace The namespace this key belongs to.
   * @param name Name of the element to store.
   * @param data The data that needs to be securely stored.
   * @param description User provided description of the entry.
   * @param properties Metadata associated with the data
   * @throws IOException If it failed to store the key in the store.
   */
  @Override
  public void putSecureData(String namespace, String name, byte[] data, String description,
                            Map<String, String> properties) throws IOException {
    String keyName = getKeyName(namespace, name);
    SecureStoreMetadata meta = SecureStoreMetadata.of(name, description, properties);
    SecureStoreData secureStoreData = new SecureStoreData(meta, data);
    writeLock.lock();
    try {
      if (keyStore.containsAlias(keyName)) {
        throw new IOException("The key " + name + " under namespace " + namespace + "already exists.");
      }
      keyStore.setKeyEntry(keyName, new KeyStoreEntry(secureStoreData, meta), password, null);
      // Attempt to persist the store.
      flush();
    } catch (KeyStoreException e) {
      // We failed to store the key in the key store. Throw an IOException.
      throw new IOException("Failed to store the key. ", e);
    } finally {
      writeLock.unlock();
    }
  }

  /**
   * Deletes the element with the given name.
   * @param namespace The namespace this key belongs to.
   * @param name Name of the element to be deleted.
   */
  @Override
  public void deleteSecureData(String namespace, String name) throws IOException {
    String keyName = getKeyName(namespace, name);
    Key key = null;
    writeLock.lock();
    try {
      key = deleteFromStore(keyName, password);
      // Attempt to persist the store. If this fails then the keystore file will have an extra key
      // that the in-memory store does not. This will be remedied the next time a flush happens.
      // If another flush does not happen and the system is restarted, the only time that file is read,
      // then we will have an extra key.
      flush();
    } catch (IOException ioe) {
      if (key != null) {
        try {
          keyStore.setKeyEntry(keyName, key, password, null);
        } catch (KeyStoreException e) {
          ioe.addSuppressed(e);
          throw ioe;
        }
      }
      throw ioe;
    } catch (UnrecoverableKeyException | NoSuchAlgorithmException | KeyStoreException e) {
        throw new IOException("Failed to delete the key. ", e);
    } finally {
      writeLock.unlock();
    }
  }

  /**
   * List of all the entries in the secure store.
   * @return A list of {@link SecureStoreMetadata} objects representing the data stored in the store.
   * @param namespace The namespace this key belongs to.
   */
  @Override
  public List<SecureStoreMetadata> listSecureData(String namespace) throws IOException {
    readLock.lock();
    try {
      Enumeration<String> aliases = keyStore.aliases();
      List<SecureStoreMetadata> list = new ArrayList<>();
      String prefix = namespace + NAME_SEPARATOR;
      while (aliases.hasMoreElements()) {
        String alias = aliases.nextElement();
        // Filter out elements not in this namespace.
        if (alias.startsWith(prefix)) {
          list.add(getSecureStoreMetadata(alias));
        }
      }
      return list;
    } catch (KeyStoreException e) {
      throw new IOException("Failed to get the list of elements from the secure store.", e);
    } finally {
      readLock.unlock();
    }
  }

  /**
   * @param namespace The namespace this key belongs to.
   * @param name Name of the data element.
   * @return An object representing the securely stored data associated with the name.
   */
  @Override
  public SecureStoreData getSecureData(String namespace, String name) throws IOException {
    String keyName = getKeyName(namespace, name);
    readLock.lock();
    try {
      if (!keyStore.containsAlias(keyName)) {
        throw new IOException(name + " not found in the secure store.");
      }
      Key key = keyStore.getKey(keyName, password);
      return ((KeyStoreEntry) key).getData();
    } catch (NoSuchAlgorithmException | UnrecoverableKeyException | KeyStoreException e) {
      throw new IOException("Unable to retrieve the key " + name, e);
    } finally {
      readLock.unlock();
    }
  }

  private Key deleteFromStore(String name, char[] password) throws KeyStoreException,
    UnrecoverableKeyException, NoSuchAlgorithmException {
    writeLock.lock();
    try {
      Key key = null;
      if (keyStore.containsAlias(name)) {
        key = keyStore.getKey(name, password);
        keyStore.deleteEntry(name);
      }
      return key;
    } finally {
      writeLock.unlock();
    }
  }

  /**
   * Returns the metadata for the element identified by the given name.
   * @param name Name of the element
   * @return An object representing the metadata associated with the element
   */
  private SecureStoreMetadata getSecureStoreMetadata(String name) throws IOException {
    readLock.lock();
    try {
      if (!keyStore.containsAlias(name)) {
        throw new IOException("Metadata for " + name + " not found in the secure store.");
      }
      Key key = keyStore.getKey(name, password);
      return ((KeyStoreEntry) key).getMetadata();
    } catch (NoSuchAlgorithmException | UnrecoverableKeyException | KeyStoreException e) {
      throw new IOException("Unable to retrieve the metadata for " + name, e);
    } finally {
      readLock.unlock();
    }
  }

  private static Path constructNewPath(Path path) {
    return path.resolveSibling(path.getFileName() + "_NEW");
  }

  private static void loadFromPath(KeyStore keyStore, Path path, char[] password)
    throws IOException {
    try (InputStream in = new DataInputStream(Files.newInputStream(path))) {
      keyStore.load(in, password);
    } catch (NoSuchAlgorithmException | CertificateException e) {
      throw new IOException("Unable to load the Secure Store. ", e);
    }
  }

  /**
   * Initialize the keyStore.
   *
   * @throws IOException If there is a problem reading or creating the keystore.
   */
  private static KeyStore locateKeystore(Path path, final char[] password) throws IOException {
    Path newPath = constructNewPath(path);
    KeyStore ks;
    try {
      ks = KeyStore.getInstance(SCHEME_NAME);
      Files.deleteIfExists(newPath);
      if (Files.exists(path)) {
        loadFromPath(ks, path, password);
      } else {
        Path parent = path.getParent();
        if (!Files.exists(parent)) {
          Files.createDirectories(parent);
        }
        // We were not able to load an existing key store. Create a new one.
        ks.load(null, password);
        LOG.info("New Secure Store initialized successfully.");
      }
    } catch (KeyStoreException | CertificateException | NoSuchAlgorithmException e) {
      throw new IOException("Can't create Secure Store. ", e);
    }
    return ks;
  }

  /**
   * Persist the keystore on the file system.
   *
   * During the flush the steps are
   *  1. Delete the _NEW file if it exists, it will exist only if something had failed in the last run.
   *  2. Try to write the current keystore in a _NEW file.
   *  3. If something fails then revert the key store to the old state and throw IOException.
   *  4. If everything is OK then rename the _NEW to the main file.
   *
   */
  private void flush() throws IOException {
    Path newPath = constructNewPath(path);
    writeLock.lock();
    try {
      // Might exist if a backup has been restored etc.
      Files.deleteIfExists(newPath);

      // Flush the keystore, write the _NEW file first
      writeToKeyStore(newPath);
      // Do Atomic rename _NEW to CURRENT
      Files.move(newPath, path, ATOMIC_MOVE, REPLACE_EXISTING);
    } finally {
      writeLock.unlock();
    }
  }

  private void writeToKeyStore(Path newPath) throws IOException {
    try (OutputStream fos = new DataOutputStream(Files.newOutputStream(newPath))) {
      keyStore.store(fos, password);
    } catch (KeyStoreException e) {
      throw new IOException("Can't store keystore.", e);
    } catch (NoSuchAlgorithmException e) {
      throw new IOException("No such algorithm storing keystore.", e);
    } catch (CertificateException e) {
      throw new IOException("Certificate exception storing keystore.", e);
    }
  }

  private static String getKeyName(final String namespace, final String name) {
    return namespace + NAME_SEPARATOR + name;
  }
}
