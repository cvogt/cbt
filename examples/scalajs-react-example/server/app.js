var express = require('express');
var https = require('https');

var app = express();

app.get('/data', function(req, res){

  var request = https.get(
    //'https://api.instagram.com/v1/media/popular?client_id=642176ece1e7445e99244cec26f4de1f&callback=?',
    'https://pixabay.com/api/?key=2741116-9706ac6d4a58f2b5416225505&q=yellow+flowers&image_type=photo',
    function(response) {
      var body = "";
      response.on('data', function(data) {
        body += data;
      });
      response.on('end', function() {
        console.log(body);
        try {
          res.send(JSON.parse(body));
        } catch (e) {
          return console.error(e);
        }
      });
    }
  );
  request.on('error', function(e) {
    console.log('Problem with request: ' + e.message);
  });
  request.end();
});

app.use(express.static(__dirname + '/public'));

var server = app.listen(3000, function () {
  var host = server.address().address;
  var port = server.address().port;

  console.log('Example app listening at http://%s:%s', host, port);
});
