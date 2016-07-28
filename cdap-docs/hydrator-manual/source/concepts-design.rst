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
#. **Data publishing:** saving the results of the transformation, either as additional data to a
   data *sink* or to a report
#. **Post-run operations:** any actions required once the pipeline run has completed, such
   as emailing notifications or cleaning up resources, regardless if the pipeline run
   succeeded or failed
   
Different plugins are available to provide functionality for each stage.

Data and Control Flow
---------------------
Processing in the pipeline is governed by two aspects: **data** and **control** flow.

**Data flow** is the movement of data, in the form of records, from one step of a pipeline
to another. When data arrives at a stage, it triggers that stage's processing of the data
and then the transference of results (if any) to the next stage.

**Control flow** is a parallel process that triggers a stage based on the result from
another process, independent of the pipeline. Currently, control flow can be applied to
the initial and final stages of a pipeline, with a post-run stage available after each
pipeline run, successful or otherwise.

Logical versus Physical Pipelines
---------------------------------

  .. include:: /how-hydrator-works.rst
     :start-after: .. _cask-hydrator-how-hydrator-works-logical-start:
     :end-before:  .. _cask-hydrator-how-hydrator-works-logical-end:

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
Similar to other CDAP applications, pipelines have a lifecycle, and can be managed and
controlled using the tools supplied by CDAP, such as the
:ref:`Cask Hydrator UI <cask-hydrator-running-pipelines-within-hydrator>`,
the :ref:`CDAP UI <cdap-ui>`, the :ref:`CDAP CLI <cdap-cli>`, and command line tools,
using the :ref:`Lifecycle HTTP RESTful API <http-restful-api-lifecycle-start>`.

.. _cask-hydrator-introduction-what-is-a-plugin:

Plugins
=======
Data *sources*, transformations (called *transforms* for short), and data *sinks* are
generically referred to as a *plugin*. Plugins provide a way to extend the functionality
of existing artifacts. An application can be created with the existing plugins included
with CDAP or, if a user wishes, they can write a plugin to add their own capability.

Properties
----------
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

