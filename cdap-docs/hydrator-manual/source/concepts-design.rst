.. meta::
    :author: Cask Data, Inc.
    :copyright: Copyright © 2016 Cask Data, Inc.

.. _cask-hydrator-concepts-design:

========================
Introduction to Hydrator
========================

Cask Hydrator (or simply, *Hydrator*) is a self-service, reconfigurable, extendable framework to
develop, run, automate, and operate **data pipelines** on Hadoop. Completely open source, it
is licensed under the Apache 2.0 license.

Hydrator is an extension to CDAP and includes the *Cask Hydrator Studio*, a visual
drag-and-drop interface for building data pipelines from an included library of pre-built
plugins.

Hydrator provides an operational view of the resulting pipeline that allows for lifecycle
control and monitoring of the metrics, logs, and other runtime information. The pipeline
can also be run directly in CDAP with tools such as the CDAP UI, the CDAP CLI, or command
line tools.

Pipelines
=========
Pipelines are applications |---| specifically for the processing of data flows |---|
created from artifacts. 

An **artifact** is an "application template". A pipeline application is created by CDAP by
using a **configuration file** that defines the desired application, along with whichever artifacts are
specified inside the configuration. Artifacts for creating data pipelines are supplied
with CDAP.

Stages and Plugins
------------------
A pipeline can be viewed as consisting of a series of *stages*. Each stage is a usage
of a *plugin*, an extension to CDAP that provides a specific functionality.

A stage's configuration properties describes what that plugin is to do (read from a
stream, write to a table, run a script), and is dependent on the particular plugin used.

All stages are connected together in a directed acyclic graph (or *DAG*), which is
shown in *Hydrator Studio* and in CDAP as a connected series of icons:

.. image:: /_images/forkInPipeline.png
   :width: 6in
   :align: center

The general progression in a pipeline is:

1. **Pre-run operations:** any actions required before the pipeline can actually run, such
   as preparing resources
#. **Data acquisition:** obtaining data from a source or sources
#. **Data transformation:** manipulating the data acquired from the sources
#. **Data publishing:** saving the results of the transformation, either as additional data or a report
#. **Post-run operations:** any actions required once the pipeline run has completed, such
   as emailing notifications or cleaning up resources, regardless if the pipeline run
   succeeded or failed

Data Flow and Control Flow
--------------------------
Processing in the pipeline is governed by two aspects:

- Data flow
- Control flow

**Data flow** is the movement of data, in the form of records, from one step of a pipeline
to another. When data arrives at a stage, it triggers that stage's processing of the data
and then the transference of results (if any) to the next stage.

**Control flow** is a parallel process that triggers a stage based on the result from
another process, independent of the pipeline. Currently, control flow can be applied to
the initial and final stages of a pipeline, with a post-run stage available after each
pipeline run, successful or otherwise.

Types of Pipelines
------------------
The data flows of a pipeline can be either **batch** or **real-time,** and a variety of
processing paradigms (MapReduce or Spark) can be used.

**Batch applications** can be scheduled to run periodically using a cron expression and can
read data from batch sources using a MapReduce job. The batch application then performs
any of a number of optional transformations before writing to one or more batch sinks.

**Real-time applications** are designed to poll sources periodically to fetch the data,
perform any optional transformations required, and then write to one or more real-time
sinks.


Pipeline Lifecycle
------------------
Similar to other CDAP applications, pipelines have a lifecycle, and can be managed and controlled
using the tools supplied by CDAP.


Logical versus Physical Pipelines
---------------------------------


.. rubric:: **Sidebar:** *Logical versus Physical Pipelines*

.. container:: inline-sidebar

  .. include:: /how-hydrator-works.rst
     :start-after: .. _cask-hydrator-how-hydrator-works-logical-start:
     :end-before:  .. _cask-hydrator-how-hydrator-works-logical-end:


Creating Pipelines
------------------
Pipelines are created from artifacts. A number of artifacts are supplied with CDAP, and
custom artifacts can be created by developers. An artifact is a blueprint or template that
|---| with the addition of a configuration file |---| is used to create an application.

A pipeline application is created by preparing a configuration that specifies the artifact
and which source, transformations (also known as transforms), and sinks are
to be used to create the application. 

The sources, transformations, and sinks are packaged as extensions to CDAP known as
**plugins**, and can include actions to be taken at the start of pipeline run, at the end,
and after the run has been completed. The plugins can be either
those that are packaged as part of CDAP or ones that have been installed separately.

The configuration can either be written as a JSON
file or, in the case of the Hydrator Studio, specified in-memory.

CDAP currently provides two artifacts |---| ``cdap-data-pipeline`` and ``cdap-etl-realtime``,
referred to as system artifacts |---| which can be used to create different kinds of
applications that work in either batch (``cdap-data-pipeline``) or real-time
(``cdap-etl-realtime``). (A third system artifact, ``cdap-etl-batch`` has been deprecated
and replaced by the ``cdap-data-pipeline`` artifact, as of CDAP 3.5.0.)

An additional system artifact (``cdap-etl-lib``) provides common resources for the other
system artifacts, and can be used by developers of custom plugins.

Pipelines can be created using Cask Hydrator's included visual editor (*Cask Hydrator
Studio*), using command-line tools such the CDAP CLI and ``curl``, or programmatically
with scripts or Java programs.

Pipeline Lifecycle
------------------
Similar to other CDAP applications, pipelines have a lifecycle, and can be managed and controlled
using the tools supplied by CDAP.


.. _cask-hydrator-introduction-what-is-a-plugin:

Plugins
=======
Sources, transformations (called *transforms* for short), and sinks are generically
referred to as a *plugin*. Plugins provide a way to extend the functionality of existing
artifacts. An application can be created with the existing plugins included with CDAP or,
if a user wishes, they can write a plugin to add their own capability.

Some plugins |---| such as the *JavaScript*, *Python Evaluator*, and *Validator*
transforms |---| are designed to be customized by end-users with their own code from
within Hydrator Studio. For instance, you can create your own data validators either by
using the functions supplied in the CoreValidator plugin or by implementing and supplying
your own custom validation function.

Types of Plugins
----------------
These are the basic *plugin types* in |cask-hydrator-version|:

- Action
- Aggregator
- Batch Sink
- Batch Source
- Compute
- Model
- Post-run Action (called after the pipeline has run)
- Real-time Sink
- Real-time Source
- Shared
- Transformation (Transform)

Additional types of plugins are under development, and developers can create and
add their own plugins and plugin types.

The batch sources can write to any batch sinks that are available and real-time sources
can write to any real-time sinks. Transformations work with either sinks or sources.
Transformations can use validators to test data and check that it follows user-specified
rules. Other plugin types may be restricted as to which plugin (and artifact) that they
work with, depending on the particular functionality they provide.

For instance, certain *model* (the *NaiveBayesTrainer*) and *compute* (the
*NaiveBayesClassifier*) plugins only work with batch pipelines.

*Action* plugins (supported only in pipelines based on the ``cdap-data-pipeline`` artifact) can
be added to run either before a source or after a sink. A "post-run" action plugin can be
specified that runs after the entire pipeline has run.

A reference lists and describes all :ref:`plugins included with CDAP <cask-hydrator-plugins>`.

Creating Plugins
----------------
Developers are free to create and add not only their own custom plugins, but their own plugin types.
Details on plugin development are covered in :ref:`cask-hydrator-developing-plugins`.
 
Plugin Templates
----------------
Within :ref:`Hydrator Studio <cask-hydrator-introduction_hydrator_studio>`, you can create
*plugin templates:* customized versions of a plugin that are reusable, and can contain
pre-configured settings.

Setting can be locked so that they cannot be altered when they are eventually used.

Once a plugin template has been created, it can be edited and deleted at a later time.

Changes to a plugin template do not affect any pipelines created using that template, as
those pipelines are created from the artifacts as specified in the plugin template at the
time of creation of the pipeline.


Properties
==========
Each stage in a pipeline represents the configuration of a specific plugin, and that
configuration usually requires that certain properties be specified. At a minimum, a
unique name for the stage and the plugin being used is required, with any additional
properties required dependent on the particular plugin used.

See the :ref:`reference section <cask-hydrator-plugins>` for details on the properties
required and supported for each plugin.


Schema
======
Each stage of a pipeline that emits data (basically, all stages except for *actions* and
*sinks*) emits data with a schema that is set for that stage. Schemas need to match
appropriately from stage to stage, and controls within *Hydrator Studio* allow the
propagation of a schema to subsequent stages.

The schema allows you to control which fields and their types are used in all stages of
pipeline. Certain plugins require specific schemas, and transform plugins are available to
convert data to required formats and schemas.


Macros
======
You may want to create a pipeline that has several configuration settings that are not
known at pipeline creation time, but that are set at the start of the each pipeline run.

For instance, you might want a pipeline that reads from a database (a source) and writes
to a table (a sink). The name of the database source and name of the table sink might
change from run to run and you need to specify those values as input before starting a
run.

You might want to create a pipeline with a particular action at the start of the run.
The action could, based on some logic, provide the name of the database to use as a source
and the name of the table to write as a sink. The next stage in the pipeline might use
this information to read and write from appropriate sources and sinks.

To do this, Hydrator supports the use of macros that will, at runtime, will be evaluated
and substituted for. The macros support recursive (nested) expansion and use a simple
syntax.

Details of usage and examples are explained in the section on :ref:`runtime arguments and
macros <cask-hydrator-runtime-arguments-macros>`.


.. _cask-hydrator-introduction_hydrator_studio:

Hydrator Studio
===============
Hydrator supports end-users with self-service batch and real-time data ingestion combined
with ETL (extract-transform-load), expressly designed for the building of Hadoop data
lakes and data pipelines. Called *Cask Hydrator Studio*, it provides for CDAP users a
seamless and easy method to configure and operate pipelines from different types of
sources and data using a visual editor.

You drag and drop sources, transformations, sinks, and other plugins to configure a pipeline:

.. figure:: _images/hydrator-studio.png
   :figwidth: 100%
   :width: 6in
   :align: center
   :class: bordered-image-top-margin

   **Cask Hydrator Studio:** Visual editor showing the creation of an ETL pipeline

Once completed, Hydrator provides an operational view of the resulting pipeline that allows for
monitoring of metrics, logs, and other runtime information:

.. figure:: _images/hydrator-pipelines.png
   :figwidth: 100%
   :width: 6in
   :align: center
   :class: bordered-image

   **Cask Hydrator Pipelines:** Administration of created pipelines showing their current status

Hydrator Studio Tips
--------------------
*[Note: this section may be best elsewhere, but for now this will keep it in a visible location.]*

- After clicking on a node, a dialog comes up to allow for **configuring of the node**. As any
  changes are automatically saved, you can just close the dialog by either hitting the close
  button (an *X* in the upper-right corner), the *escape* key on your keyboard, or clicking
  outside the dialog box.
  
- To **edit a connection** made from one node to another node, you can remove the
  connection by clicking the end with the arrow symbol (click on the white dot) and dragging
  it off of the target node.

- All **pipelines must have unique names**, and a pipeline **cannot be saved over an existing
  pipeline** of the same name. Instead, increment the name (from *Demo* to *Demo-1*) with
  each new cloning of a pipeline.
