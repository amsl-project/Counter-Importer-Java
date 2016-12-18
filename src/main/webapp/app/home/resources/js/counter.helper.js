angular.module('counterHelperModule', [])

.service('counterSearch', ['$rootScope', function($rootScope) {
	this.counterSupplier = '';
	this.sushiSetting = '';
	this.showEmptySupplier = true;
	
	this.setCounterSupplier = setCounterSupplier;
	function setCounterSupplier(searchString) {
		this.counterSupplier = searchString;
		$rootScope.$broadcast('counterSearchUpdate');
	}
	this.setSushiSetting = setSushiSetting;
	function setSushiSetting(searchString) {
		this.sushiSetting = searchString;
		$rootScope.$broadcast('counterSearchUpdate');
	}
	this.showEmptySuppliers = showEmptySuppliers;
	function showEmptySuppliers(bool) {
		this.showEmptySupplier = bool;
		$rootScope.$broadcast('counterSearchUpdate');
	}

}])