/**
 * Created by andy on 6/3/16.
 */
var grpc = require('/usr/local/lib/node_modules/grpc');
var proto = grpc.load('./src/main/protobuf/chat.proto');
var stub = new proto.io.bigfast.Chat('localhost:50051', grpc.credentials.createInsecure());

// Test create channel
stub.createChannel({name: "Hi", description: "foo"}, function(err, resp) {console.log("err:" + err); console.log(resp);});

// Test bidirectional channel
var call = stub.subscribeEvents();
call.on('data', function(data) {console.log(data);});

call.write({userId: 1, appId: 2, auth: 3});
