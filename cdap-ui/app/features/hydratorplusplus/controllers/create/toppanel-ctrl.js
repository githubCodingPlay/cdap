/*
 * Copyright © 2015-2016 Cask Data, Inc.
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

class HydratorPlusPlusTopPanelCtrl{
  constructor($stateParams, HydratorPlusPlusConfigStore, HydratorPlusPlusConfigActions, $uibModal, HydratorPlusPlusConsoleActions, DAGPlusPlusNodesActionsFactory, GLOBALS, myHelpers, HydratorPlusPlusConsoleStore, myPipelineExportModalService, $timeout, $scope) {
    this.consoleStore = HydratorPlusPlusConsoleStore;
    this.myPipelineExportModalService = myPipelineExportModalService;
    this.HydratorPlusPlusConfigStore = HydratorPlusPlusConfigStore;
    this.GLOBALS = GLOBALS;
    this.HydratorPlusPlusConfigActions = HydratorPlusPlusConfigActions;
    this.$uibModal = $uibModal;
    this.HydratorPlusPlusConsoleActions = HydratorPlusPlusConsoleActions;
    this.DAGPlusPlusNodesActionsFactory = DAGPlusPlusNodesActionsFactory;
    this.parsedDescription = this.HydratorPlusPlusConfigStore.getDescription();
    this.myHelpers = myHelpers;
    this.$timeout = $timeout;

    this.canvasOperations = [
      {
        name: 'Settings',
        icon: 'fa-sliders',
        fn: this.showSettings.bind(this)
      },
      {
        name: 'Export',
        icon: 'icon-export',
        fn: this.onExport.bind(this)
      },
      {
        name: 'Save',
        icon: 'icon-savedraft',
        fn: this.onSaveDraft.bind(this)
      },
      {
        name: 'Validate',
        icon: 'icon-validate',
        fn: this.onValidate.bind(this)
      },
      {
        name: 'Publish',
        icon: 'icon-publish',
        fn: this.onPublish.bind(this)
      }
    ];
    this.$stateParams = $stateParams;
    this.setState();
    this.HydratorPlusPlusConfigStore.registerOnChangeListener(this.setState.bind(this));
    this.focusTimeout = null;

    if ($stateParams.isClone) {
      this.openMetadata();
    }

    $scope.$on('$destroy', () => {
      this.$timeout.cancel(this.focusTimeout);
    });
  }
  setMetadata(metadata) {
    this.state.metadata = metadata;
  }
  setState() {
    this.state = {
      metadata: {
        name: this.HydratorPlusPlusConfigStore.getName(),
        description: this.HydratorPlusPlusConfigStore.getDescription()
      },
      viewSettings: this.myHelpers.objectQuery(this.state, 'viewSettings') || false,
      artifact: this.HydratorPlusPlusConfigStore.getArtifact()
    };
  }

  openMetadata() {
    this.metadataExpanded = true;
    this.invalidName = false;

    this.$timeout.cancel(this.focusTimeout);
    if (!this.$stateParams.isClone) {
      return;
    }
    this.$stateParams.isClone = null;
    this.focusTimeout = this.$timeout(() => {
      document.getElementById('pipeline-name-input').focus();
    });
  }
  resetMetadata(event) {
    this.setState();
    this.metadataExpanded = false;
    event.preventDefault();
    event.stopPropagation();
  }
  saveMetadata(event) {
    this.HydratorPlusPlusConfigActions.setMetadataInfo(this.state.metadata.name, this.state.metadata.description);
    if (this.state.metadata.description) {
      this.parsedDescription = this.state.metadata.description.replace(/\n/g, ' ');
      this.tooltipDescription = this.state.metadata.description.replace(/\n/g, '<br />');
    } else {
      this.parsedDescription = '';
      this.tooltipDescription = '';
    }
    this.metadataExpanded = false;
    event.preventDefault();
    event.stopPropagation();
  }
  onEnterOnMetadata(event) {
    // Save when user hits ENTER key.
    if (event.keyCode === 13) {
      this.saveMetadata(event);
      this.metadataExpanded = false;
    } else if (event.keyCode === 27) {
      // Reset if the user hits ESC key.
      this.resetMetadata();
    }
  }

  onExport() {
    this.DAGPlusPlusNodesActionsFactory.resetSelectedNode();
    let config = angular.copy(this.HydratorPlusPlusConfigStore.getDisplayConfig());
    let exportConfig = this.HydratorPlusPlusConfigStore.getConfigForExport();
    this.myPipelineExportModalService.show(config, exportConfig);
  }
  onSaveDraft() {
    this.HydratorPlusPlusConfigActions.saveAsDraft();
  }
  checkNameError() {
    let messages = this.consoleStore.getMessages() || [];
    let filteredMessages = messages.filter( message => {
      return ['MISSING-NAME', 'INVALID-NAME'].indexOf(message.type) !== -1;
    });

    this.invalidName = (filteredMessages.length ? true : false);
  }
  onValidate() {
    this.HydratorPlusPlusConsoleActions.resetMessages();
    let isStateValid = this.HydratorPlusPlusConfigStore.validateState(true);
    if (isStateValid) {
      this.HydratorPlusPlusConsoleActions.addMessage([{
        type: 'success',
        content: 'Validation success! Pipeline ' + this.HydratorPlusPlusConfigStore.getName() + ' is valid.'
      }]);
      return;
    }
    this.checkNameError();
  }
  onPublish() {
    this.HydratorPlusPlusConfigActions.publishPipeline();
    this.checkNameError();
  }
  showSettings() {
    this.state.viewSettings = !this.state.viewSettings;
  }
}

HydratorPlusPlusTopPanelCtrl.$inject = ['$stateParams', 'HydratorPlusPlusConfigStore', 'HydratorPlusPlusConfigActions', '$uibModal', 'HydratorPlusPlusConsoleActions', 'DAGPlusPlusNodesActionsFactory', 'GLOBALS', 'myHelpers', 'HydratorPlusPlusConsoleStore', 'myPipelineExportModalService', '$timeout', '$scope'];

angular.module(PKG.name + '.feature.hydratorplusplus')
  .controller('HydratorPlusPlusTopPanelCtrl', HydratorPlusPlusTopPanelCtrl);
