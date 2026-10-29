var exec = require('cordova/exec');

module.exports = {
    enforceLock: function (success, error) {
        exec(success, error, "DisplayLock", "enforceLock", []);
    }
};
module.exports = DisplayLock;
