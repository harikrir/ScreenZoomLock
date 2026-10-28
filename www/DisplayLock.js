var exec = require('cordova/exec');

var DisplayLock = {
    enforceLock: function (success, error) {
        exec(success, error, 'DisplayLockPlugin', 'enforceLock', []);
    }
};

module.exports = DisplayLock;
