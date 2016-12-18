angular.module('sushiSettingsModule', [])

.directive('sushiSettingsWidget', ["$timeout", 'Upload', function($timeout, Upload) {
    return{
    	restrict: 'E',
        templateUrl:'app/home/resources/html/sushi.setting.html',
        scope: {data: '='},
        controller: ['$scope', '$rootScope', 'counterSearch', function($scope, $rootScope, counterSearch) {
        	$scope.display = true;
        	$scope.displayContent = false;
        	$scope.toggleContent = function(){
        		$scope.displayContent = !$scope.displayContent;
        	}
        	$scope.displayFiles = false;
        	$scope.toggleFiles = function(){
        		$scope.displayFiles = !$scope.displayFiles;
        	}
        	$rootScope.$on('counterSearchUpdate', function () {
        		if($scope.data.label.toLowerCase().indexOf(counterSearch.sushiSetting.toLowerCase()) != -1){
        			$scope.display = true;
        		}else{
        			$scope.display = false;
        		}
            });
        	$scope.uploadedFiles = new Array();
        	$scope.showFiles = function(){
        		$scope.listFiles();
        		$scope.displayFiles = true;
        	}
            $scope.listFiles = function(){
            	$.getJSON("/counter/files" + $scope.data.folder, function(data) {
            		$timeout(function(){
//            			$('.hide-files-btn-' + removeSlashes($scope.data.folder)).css("display", "inline");
//            			$('.files-' + removeSlashes($scope.data.folder)).css("display", "inline");
            			$scope.uploadedFiles = data;
            		});
    			});
            }
            $scope.hideFiles = function(){
//            	$('.hide-files-btn-' + removeSlashes($scope.data.folder)).css("display", "none");
//    			$('.files-' + removeSlashes($scope.data.folder)).css("display", "none");
//            	$scope.toggleFiles();
            	$scope.displayFiles = false;
            }
            $scope.removeSlashes = removeSlashes;
            function removeSlashes(string){
            	return string.split("/").join("");
            }
            $scope.file = null;

            // upload on file select or drop
            $scope.upload = function (file, folder) {
            	console.log('.file-label-' + removeSlashes(folder));
            	$('#file-label-' + removeSlashes(folder) ).css("display", "inline");
            	Upload.upload({
            		url: '/counter/files' + folder,
            		data: {file: file,}
            	}).then(function (resp) {
            		console.log('Success ' + resp.config.data.file.name + 'uploaded. Response: ' + resp.data);
            	}, function (resp) {
            		console.log('Error status: ' + resp.status);
            	}, function (evt) {
            		var progressPercentage = parseInt(100.0 * evt.loaded / evt.total);
            		console.log('progress: ' + progressPercentage + '% ' + evt.config.data.file.name);
            		$('#file-upload-' + removeSlashes(folder)).css('width', 2*progressPercentage + "px");
            		if(progressPercentage == 100){
            			$('#file-upload-succes-' + removeSlashes(folder)).css('opacity', 1);
            			$scope.listFiles();
            		}
            	});
            };
            
            $scope.undoImport = function(file, folder){
            	file = file.split("_")[0];
            	$.getJSON("/counter/files/undo/" + folder+ "/" + file, function(data) {
            		$timeout(function(){
            			
            		});
    			});
            };
        }]
    }
}])