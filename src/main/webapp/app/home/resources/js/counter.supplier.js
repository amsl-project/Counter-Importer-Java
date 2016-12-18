angular.module('counterSupplierModule', [])

.directive('counterSupplierWidget', [function() {
    return{
    	restrict: 'E',
        templateUrl:'app/home/resources/html/counter.supplier.html',
        scope: {data: '='},
        controller: ['$scope', '$rootScope', 'counterSearch', function($scope, $rootScope, counterSearch) {
        	$scope.displayContent = false;
        	$scope.userDecidedToDisplayContent = false;
        	$scope.toggleContent = function(){
        		$scope.displayContent = !$scope.displayContent;
        		$scope.userDecidedToDisplayContent = $scope.displayContent;
        	}
        	$scope.display = true;
        	$rootScope.$on('counterSearchUpdate', function () {
        		if($scope.data.label.toLowerCase().indexOf(counterSearch.counterSupplier.toLowerCase()) != -1){
        			$scope.display = true;
        		}else{
        			$scope.display = false;
        		}
        		if($scope.display == true){
        			var noVisibleChildren = true;
        			$scope.data.sushiSettings.forEach(function(element){
        				if(element.label.toLowerCase().indexOf(counterSearch.sushiSetting.toLowerCase()) != -1){
        					noVisibleChildren = false;
                		}
        			});
        			if(noVisibleChildren == true){
        				if(counterSearch.showEmptySupplier){
        					$scope.display = true;
        				}else{
        					$scope.display = false;
        				}
    				}else{
    					if(counterSearch.sushiSetting != ""){
    						$scope.displayContent = true;
    					}else{
    						if($scope.userDecidedToDisplayContent == false){
    							$scope.displayContent = false;
    						}
    					}
    				}
        		}
//        		else{
//        			var noVisibleChildren = true;
//        			$scope.data.sushiSettings.forEach(function(element){
//        				if(element.label.toLowerCase().indexOf(counterSearch.sushiSetting.toLowerCase()) != -1){
//        					noVisibleChildren = false;
//                		}
//        			});
//        			if(noVisibleChildren == false){
//        				if(counterSearch.showEmptySupplier){
//        					$scope.display = true;
//        					$scope.displayContent = true;
//        				}else{
//        					$scope.display = false;
//        				}
//    				}
//        		}
        	});
        }]
    }
}])