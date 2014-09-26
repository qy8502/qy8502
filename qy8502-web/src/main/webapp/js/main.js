/*global require*/
'use strict';

require
		.config({
			paths : {
				jquery : 'http://lib.sinaapp.com/js/jquery/2.0.3/jquery-2.0.3',
				angular : 'http://lib.sinaapp.com/js/angular.js/angular-1.2.19/angular',
				//bootstrap : 'http://lib.sinaapp.com/js/bootstrap/3.0.0/js/bootstrap',
				bootstrap : '../bower_components/bootstrap/dist/js/bootstrap',
				uibootstrap : '../bower_components/angular-bootstrap/ui-bootstrap'
			},
			shim : {
				bootstrap : [ 'jquery' ],
				angular : {
					exports : 'angular'
				},
				uibootstrap : ['angular','bootstrap']
			}
		});

require([ 'angular', 'jquery','bootstrap','uibootstrap', 'app', 'controllers/todo',
		'directives/todoFocus' ], function(angular, $) {
	// Twitter Bootstrap 3 carousel plugin
	$("#element").carousel();
	angular.bootstrap(document, [ 'todomvc','ui.bootstrap' ]);
});
