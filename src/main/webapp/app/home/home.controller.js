(function() {
    'use strict';

    angular
        .module('amslApp')
        .controller('HomeController', HomeController);

    HomeController.$inject = ['$scope', 'Principal', 'LoginService', '$state',  'Upload', 'counterSearch', "$timeout"];

    function HomeController ($scope, Principal, LoginService, $state, Upload, counterSearch, $timeout) {
        var vm = this;

        vm.account = null;
        vm.isAuthenticated = null;
        vm.login = LoginService.open;
        vm.register = register;
        
        $scope.counterSupplierSearch = function(){
        	counterSearch.setCounterSupplier($scope.counterSupplierSearchString)
        }
        $scope.sushiSettingsSearch = function(){
        	counterSearch.setSushiSetting($scope.sushiSettingsSearchString)
        }
        $scope.showEmptySuppliers = function(){
        	counterSearch.showEmptySuppliers($scope.showEmptySuppl)
        }
        
        $scope.$on('authenticationSuccess', function() {
            getAccount();
        });

        getAccount();

        function getAccount() {
            Principal.identity().then(function(account) {
                vm.account = account;
                vm.isAuthenticated = Principal.isAuthenticated;
            });
        }
        function register () {
            $state.go('register');
        }
//        $scope.file = null;
//
//          // upload on file select or drop
//        $scope.upload = function (file, folder) {
//        	Upload.upload({
//        		url: '/counter/files' + folder,
//        		data: {file: file,}
//        	}).then(function (resp) {
//        		console.log('Success ' + resp.config.data.file.name + 'uploaded. Response: ' + resp.data);
//        	}, function (resp) {
//        		console.log('Error status: ' + resp.status);
//        	}, function (evt) {
//        		var progressPercentage = parseInt(100.0 * evt.loaded / evt.total);
//        		console.log('progress: ' + progressPercentage + '% ' + evt.config.data.file.name);
//        		$('#file-upload-' + removeSlashes(folder)).css('width', 2*progressPercentage + "px");
//        		if(progressPercentage == 100){
//        			$('#file-upload-succes-' + removeSlashes(folder)).css('opacity', 1);
//        		}
//        	});
//        };
        
        $scope.counterSuppliers = new Array();
        $.getJSON("/counter/supplier", function(data) {
        	$timeout(function() {
        		$scope.counterSuppliers = data;
        	});
		});
        
//        $scope.list = function(folder){
//        	$.getJSON("/counter/files" + folder, function(data) {
//            	$scope.counterFiles = data;
//    		});
//        }
        
        $scope.removeSlashes = removeSlashes;
        function removeSlashes(string){
        	return string.split("/").join("");
        }
          
          
    }
})();
