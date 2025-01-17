WebSocket {

	classvar m_granularity;

	*initClass {
		m_granularity = 200;
	}

	*granularity { ^m_granularity }

	*granularity_ { |gran|
		var server_instances;
		var client_instances;
		m_granularity = gran;
	}
}

WebSocketConnection
{
	var m_ptr;
	var <address;
	var <port;
	var textMessageCallback;
	var binaryMessageCallback;
	var oscMessageCallback;

	*new {
		^super.new;
	}

	// from WebSocketServer
	*newInit { |ptr|
		^this.newCopyArgs(ptr).prmBind();
	}

	// from WebSocketClient, when connected
	initialize { |ptr|
		m_ptr = ptr;
		this.prmBind();
	}

	isPtr { |ptr| ^m_ptr == ptr; }

	prmBind {
		_WebSocketConnectionBind
		^this.primitiveFailed
	}

	onTextMessageReceived_ { |callback|
		textMessageCallback = callback;
	}

	onBinaryMessageReceived_ { |callback|
		binaryMessageCallback = callback;
	}

	onOscMessageReceived_ { |callback|
		oscMessageCallback = callback;
	}

	pvOnTextMessageReceived { |message|
		textMessageCallback.value(message);
	}

	pvOnBinaryMessageReceived { |message|
		binaryMessageCallback.value(message);
	}

	pvOnOscMessageReceived { |address, arguments|
		oscMessageCallback.value(address, arguments);
	}

	writeText { |msg|
		_WebSocketConnectionWriteText
		^this.primitiveFailed
	}

	writeOsc { |... array|
		_WebSocketConnectionWriteOsc
		^this.primitiveFailed
	}

	writeOscClient { |array|
		_WebSocketConnectionWriteOsc
		^this.primitiveFailed
	}

	writeBinary { |data|
		_WebSocketConnectionWriteBinary
		^this.primitiveFailed
	}
}

WebSocketClient
{
	var m_ptr;
	var m_connection;
	var <connected;
	var m_ccb;
	var m_dcb;
	var m_http_cb;

	classvar g_instances;

	*initClass {
		g_instances = [];
		ShutDown.add({
			g_instances.do(_.free());
		})
	}

	*new {
		^super.new.wsClientCtor()
		.primCreate()
		.granularity_(WebSocket.granularity());
	}

	*instances { ^this.g_instances }

	wsClientCtor {
		connected = false;
		g_instances = g_instances.add(this);
		m_connection = WebSocketConnection();
	}

	primCreate {
		_WebSocketClientCreate
		^this.primitiveFailed
	}

	connect { |ip, port|
		_WebSocketClientConnect
		^this.primitiveFailed
	}

	disconnect {
		this.primDisconnect();
		if (m_dcb.notNil()) {
			m_dcb.value();
		}
	}

	granularity_ { |milliseconds|
		_WebSocketClientSetGranularity
		^this.primitiveFailed
	}

	primDisconnect {
		_WebSocketClientDisconnect
		^this.primitiveFailed
	}

	onConnected_ { |callback|
		m_ccb = callback;
	}

	onDisconnected_ { |callback|
		m_dcb = callback;
	}

	pvOnConnected { |ptr|
		m_connection.initialize(ptr);
		connected = true;
		m_ccb.value();
	}

	pvOnDisconnected {
		connected = false;
		m_dcb.value();
	}

	onTextMessageReceived_ { |callback|
		m_connection.onTextMessageReceived_(callback);
	}

	onBinaryMessageReceived_ { |callback|
		m_connection.onBinaryMessageReceived_(callback);
	}

	onOscMessageReceived_ { |callback|
		m_connection.onOscMessageReceived_(callback);
	}

	onHttpReplyReceived_ { |callback|
		m_http_cb = callback;
	}

	pvOnHttpReplyReceived { |ptr|
		var reply = HttpRequest.newFromPrimitive(ptr);
		m_http_cb.value(reply);
		reply.free();
	}

	writeText { |msg|
		m_connection.writeText(msg);
	}

	writeOsc { |...array|
		m_connection.writeOscClient(array);
	}

	writeBinary { |data|
		m_connection.writeBinary(data);
	}

	request { |req|
		_WebSocketClientRequest
		^this.primitiveFailed
	}

	free {
		g_instances.remove(this);
		m_connection = nil;
		this.primFree();
	}

	primFree {
		_WebSocketClientFree
		^this.primitiveFailed
	}
}

Http
{
	*ok        { ^200 }
	*notFound  { ^404 }
}

HttpRequest
{
	var m_ptr;
	var <>uri;
	var <>query;
	var <>mime;
	var <>body;

	*newFromPrimitive { |ptr|
		^this.newCopyArgs(ptr).reqCtor()
	}

	reqCtor {
		_HttpRequestBind
		^this.primitiveFailed
	}

	*new { |uri = '/', query = "", mime = "", body = ""|
		^this.newCopyArgs(0x0, uri, query, mime, body)
	}

	reply { |code, text, mime = ""|
		_HttpReply
		^this.primitiveFailed
	}

	replyJson { |json|
		// we assume code is 200 here
		this.reply(200, json, "application/json");
	}

	free {
		_HttpRequestFree
		^this.primitiveFailed
	}
}

WebSocketServer
{
	var m_ptr;
	var m_port;
	var m_connections;
	var m_ncb;
	var m_dcb;
	var m_hcb;

	classvar g_instances;

	*initClass {
		g_instances = [];
		ShutDown.add({
			g_instances.do(_.free());
		})
	}

	*new { |port|
		^this.newCopyArgs(0x0, port).wsServerCtor();
	}

	wsServerCtor {
		m_connections = [];
		g_instances = g_instances.add(this);
		this.prmInstantiateRun(m_port);
		this.granularity = WebSocket.granularity();
	}

	prmInstantiateRun { |port|
		_WebSocketServerInstantiateRun
		^this.primitiveFailed
	}

	granularity_ { |milliseconds|
		_WebSocketServerSetGranularity
		^this.primitiveFailed
	}

	port { ^m_port }

	at { |index|
		^m_connections[index];
	}

	numConnections {
		^m_connections.size();
	}

	writeAll { |data|
		m_connections.do(_.write(data));
	}

	onNewConnection_ { |callback|
		m_ncb = callback;
	}

	onDisconnection_ { |callback|
		m_dcb = callback;
	}

	onHttpRequestReceived_ { |callback|
		m_hcb = callback;
	}

	pvOnNewConnection { |con|
		m_connections = m_connections.add(WebSocketConnection.newInit(con));
		m_ncb.value(m_connections.last());
	}

	pvOnHttpRequestReceived { |request|
		var screq = HttpRequest.newFromPrimitive(request);
		m_hcb.value(screq);
		screq.free();
	}

	pvOnDisconnection { |cptr|
		var rem;
		m_connections.do({|wcon|
			if (wcon.isPtr(cptr)) {
				m_dcb.value(wcon);
				rem = wcon;
			}
		});

		if (rem.notNil()) {
			m_connections.removeAll(rem);
		}
	}

	free {
		g_instances.remove(this);
		this.prmFree();
	}

	prmFree {
		_WebSocketServerFree
		^this.primitiveFailed
	}
}



