/* Author:

*/

$(document).ready(function() {
	$('.social.fb').click(function() {
		mixpanel.track("FB Page Outbound Clicked");
	});
	$('.social.twitter').click(function() {
		mixpanel.track("Twitter Page Outbound Clicked");
	});
	$('.play').click(function() {
		mixpanel.track("Android Market Page Clicked");
	});
});






