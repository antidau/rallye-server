
function findRoot() {

	var loc = document.location.href;
	var pos = loc.indexOf('/',8);
	return loc.substr(0,pos+1);

}

function setup_error_handler() {

	$(document).bind("ajaxError",function(event, xhr, ajaxOptions, thrownError) {
		var readable;
		console.log(xhr.status);
		//Network error?
		if (xhr.status==0) {
			if(thrownError)
				readable = thrownError;
			else readable = xhr.statusText;
		//Wrong auth?
		} else if (xhr.status==401) { 
		
			//New login box
			display_login("Invalid login specified.");
			
			//If it was called with authajax, try to do again
			if (ajaxOptions.__myData)
				authQueue.push(ajaxOptions);
			
			
			return;
		
		//Other error
		} else {
			readable = xhr.status+" "+xhr.statusText;
			if (xhr.statusText!=thrownError)
				readable+=" "+thrownError;
		}
		alert(readable);
	});
}

var root = findRoot(); //Root path to server
var username = window.localStorage["username"]; //Auth username
var password = window.localStorage["password"]; //Auth password

if (username && password) {
	console.log(username);
	console.log(password);
} 


var partials;

var currentDisplay = null; // Currently displayed element.
var authQueue = []; //Queue of requests to make when auth finishes.


var resources = {};
var resourceQueue = {};
/**
 * Get some unchanging resource, only once per page load and return the cached result for subsequent requests
 */
function getResource(name, url, callback, transform) {
	//Already loaded?
	if (resources[name]) {
		callback(resources[name]);
		return;
	}
	//Currently loading?
	if (resourceQueue[name]) {
		resourceQueue[name].push(callback);
		return;
	}
	//Else start new request.
	resourceQueue[name] = [callback];
	authajax({
		'url':url,
		'dataType': 'json'		
	},function(res) {

		if (transform) res = transform(res);
		
		resources[name] = res;
		queue = resourceQueue[name];
		delete resourceQueue[name];
		
		
		queue.forEach(function(elem) {
			elem(res);
		});
		
	})
}

function getUsers(callback) {
	getResource("users",root+"rallye/users",callback, function (arr) {
		var res = {};
		arr.forEach(function (elem) {
			res[elem.userID] = elem;
		});
		return res;
	});
}


function getGroups(callback) {
	getResource("groups",root+"rallye/groups",callback, function (arr) {
		var res = {};
		arr.forEach(function (elem) {
			res[elem.groupID] = elem;
		});
		return res;
	});
}

/**
 * wrapper for jQuery.ajax, adding auth information
 */
function make_auth_request(q) {
	console.log("making authed request for "+q.url);
	q.username = username;
	q.password = password;
	$.ajax(q)
		.done(q.__myData.done)
		.error(q.__myData.fail)
		.always(q.__myData.always);
}

function display_login(msg) {
	if (!msg) msg = "Please log in.";
	console.log("Displaying login box");
	
	var template = $('#templ_logindialog').html();
	var html = Mustache.to_html(template,msg);
	
	$("#main").html(html);

}

function submit_login() {
	console.log("submitting login");
	username = $("#username").val();
	password = $("#password").val();
	
	if (!username || !password) {
		display_login("Please enter username and password.");
		return;
	}
	
	var remember = $("#remember").is(':checked');
	console.log($("#remember"));
	
	console.log("new auth: "+username+" "+password);

	if (remember) {
	 	$("#logout").css("display","");
		window.localStorage["username"] = username;
		window.localStorage["password"] = password;
	}
	
	$("#main").html("Logging in...");
	
	
	send_socket_login();
	
	return false;
}

/**
 * Checks if auth info has been supplied, makes an jQuery ajax request if yes, displays auth if not
 */
function authajax(obj, done, fail, always) {

	console.log("making request for "+obj.url);
	obj.__myData = {
		done: done,
		fail: fail,
		always: always
	};
			

	//Is there auth info?
	if (!username || ! password) {
		console.log("but no auth specified");
	
		//Are we already displaying auth?
		if (currentDisplay != "auth") {
			console.log("therefore showing login");
			display_login();	
		} else 
			console.log("already showing login");
		//Queue request
		authQueue.push(obj);
	
	} else
		make_auth_request(obj);

}

function kill_auth() {
	console.log("killing auth");
	username = null;
	password = null;
	window.localStorage.clear();
	window.location.reload()
	return false;
}



function get_system_info(callback) {
	$.ajax({
		url: root+"rallye/system/info",
		dataType: "json",
	}).done(callback);
}

function get_chatrooms(callback) {
	authajax({
		url: root+"rallye/chatrooms",
		dataType: "json"
	},callback);
}

function leadZero(num) {
	if (num<10)
		return '0'+num;
	return num;
}
function formatTime(unixTimestamp) {
    var dt = new Date(unixTimestamp * 1000);

	var day = leadZero(dt.getDay());
	var month = leadZero(dt.getMonth());
	var year = dt.getFullYear();
	
    var hours = leadZero(dt.getHours());
    var minutes = leadZero(dt.getMinutes());
    var seconds = leadZero(dt.getSeconds());


    return day+"."+month+"."+year+" "+hours + ":" + minutes + ":" + seconds;
}       

function get_nodes(callback) {
	$.ajax({
		url: root+"rallye/map/nodes",
		dataType: "json"
	}).done(callback);
}
var roomID;
function load_chatroom(id) {
	
	authajax({
		url: root+"rallye/chatrooms/"+id,
		dataType: "json"
	},function (res) {getUsers(function(users) {getGroups(function(groups) {
		roomID = id;
		history.pushState(null, "", root+"client/?chatroom="+id);
		

		var template = $('#templ_chat').html();
		
		var content = res.map(function(chat) {
			var user = users[chat.userID];
			var name = user?user.name:chat.userID;
			var group = groups[chat.groupID];
			var groupName = group?group.name:chat.groupID;
			var picture = null;
			var pictureBig = null
			if (chat.pictureID) {
				picture = root+"rallye/pics/"+chat.pictureID+"/thumb";
				pictureBig = root+"rallye/pics/"+chat.pictureID+"/std";
			}
			var ava = root+"rallye/groups/"+chat.groupID+"/avatar";
			
			return { name: name, message: chat.message, picture: picture, pictureBig: pictureBig,
				avatar: ava, time: formatTime(chat.timestamp), group: groupName};
		
		});
		var html = Mustache.to_html(template,content,partials);

		$("#main").html(html).removeAttr("style");
	});});});
	
	
	return false;
}

var urlParams;
function load_querystring() {
	var match,
	pl     = /\+/g,  // Regex for replacing addition symbol with a space
	search = /([^&=]+)=?([^&]*)/g,
	decode = function (s) { return decodeURIComponent(s.replace(pl, " ")); },
	query  = window.location.search.substring(1);
	
	urlParams = {};
	while (match = search.exec(query))
		urlParams[decode(match[1])] = decode(match[2]);
}
function getParameterByName(name) {
    return urlParams[name];
}

google.maps.visualRefresh = true;
function load_map() {
	console.log("loading map");
    var mapOptions = {
	center: new google.maps.LatLng(49.87, 8.65),
    zoom: 14,
    mapTypeId: google.maps.MapTypeId.ROADMAP
  };
  $("#main").css("right","0px").css("height","600px");
  $("#map-canvas").css("width","100%").css("height","100%");
  var map = new google.maps.Map(document.getElementById("main"),
      mapOptions);
  

	history.pushState(null, "", root+"client/?map");
  
  	get_nodes(function(nodes) {
  		function addNode() {
  				var node = nodes.pop();
      		  new google.maps.Marker({
      		      position: new google.maps.LatLng(node.position.latitude,node.position.longitude),
      		      map: map,
      		    animation: google.maps.Animation.DROP,
      		      title:node.name
      		  });
      		  if (nodes.length>0)
      			  setTimeout(addNode,10);
  			
  		}
  		addNode();
  	});
  
  
	return false;
}

function submit_message() {
	var sendbutton = $("#sendbutton");
	sendbutton.attr("disabled","disabled");
	sendbutton.html('<img src="load.gif" />');
	var message = $("#message").val();
	
	authajax({
		url: root+"rallye/chatrooms/"+roomID,
		method: "PUT",
		contentType: "application/json",
		data: JSON.stringify({
			"message": message
		})
	},function(res){ //Success
		$("#message").val("");
	},function() { //Error
		
	}, function() { //Always
		sendbutton.html("Send");
		sendbutton.removeAttr("disabled");
		
	});
	
	return false;

}

var socket_open = false;
var socket;
function send_socket_login() {
	socket.send(JSON.stringify({
		type: 'login',
		username: username,
		password: password
	}));
}
function setup_socket() {
	
	var url = root.replace("http","ws") + "rallye/push";
	var status = $("#status");
	status.text("Socket started at "+url);
	
	socket = new WebSocket(url);
	
	socket.onopen = function(){
		socket_open = true;
		status.text("Socket opened");
		
		if (username && password)
			send_socket_login();
	}
	socket.onmessage = function(msg){  
		var data = JSON.parse(msg.data);
		console.log(data);
		status.text("Socket working");
	    
	    switch(data.type) {
	    case "login":
	    	console.log("login returned: "+data.state);
	    	if (data.state=="ok") {

	    		//Empty global queue
	    		queue = authQueue;
	    		authQueue = [];
	    		
	    		//Do the requests
	    		console.log("requesting stuff from auth queue");
	    		queue.forEach(make_auth_request);
	    	}
	    	if (data.state=="fail") {
				display_login("Invalid login specified. (socket)");
	    	} else if (data.state=="error") {
	    		alert("server is broken.");
	    	}
	    	break;
	    case "newMessage":
	    	var chat = JSON.parse(data.payload);
	    	console.log(chat);
	    	
	    	if (roomID==chat.chatroomID) {
	    	
		    	getUsers(function(users) { getGroups(function(groups) {
				
					var user = users[chat.userID];
					var name = user?user.name:chat.userID;
					var group = groups[chat.groupID];
					var groupName = group?group.name:chat.groupID;
					var picture = null;
					var pictureBig = null
					if (chat.pictureID) {
						picture = root+"rallye/pics/"+chat.pictureID+"/thumb";
						pictureBig = root+"rallye/pics/"+chat.pictureID+"/std";
					}
					var ava = root+"rallye/groups/"+chat.groupID+"/avatar";
			
					var content =  { name: name, message: chat.message, picture: picture, pictureBig: pictureBig,
						avatar: ava, time: formatTime(chat.timestamp), group: groupName};
				
				
		    		var html = Mustache.to_html(partials.line,content);
		    		$("#chatentries").append(html);
		    		
		    	});});
	    	}
	    	
	    	break;
	    default:
	    	alert("unrecognized message: "+data.type);
	    }
	}   
	
	socket.onclose = function() {
	
		status.text("Socket closed");
	}
	
}

var chatrooms;
$(function () {

	load_querystring();

	partials = {
			'line': $("#templ_chatline").html()
	};

	
	setup_error_handler();


	get_system_info(function(res) {
		$("#servername").html("<b>"+Mustache.escape(res.name)+"</b><br>"+Mustache.escape(res.description));
		document.title=res.name;
	});

	get_chatrooms(function(res) {
		chatrooms = res;
		
		var roomLinks = res.reduce(function(list,room) {
		
			var link = '<a href="#" onclick="return load_chatroom('+room.chatroomID+');">'+room.name+'</a><br>';
			return list + link;
		
		},"");
		roomLinks = roomLinks + '<a href="#" onclick="return load_map();">Map</a>'
		$("#left").html(roomLinks);
	});
	
	
	setup_socket();
	
	var lastRoom = getParameterByName("chatroom");
	if (lastRoom) load_chatroom(lastRoom);
	
	var map = getParameterByName("map");
	if (map!=null)
		load_map();
	
	
	getUsers(function(l) {console.log(l)});

});
