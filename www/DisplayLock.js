var exec = require('cordova/exec');

var DisplayLock = {
    enforceLock: function (success, error) {
        exec(success, error, 'DisplayLock', 'enforceLock', []);
    }
};

module.exports = DisplayLock;
