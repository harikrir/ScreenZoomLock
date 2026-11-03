var exec = require('cordova/exec');

var DisplayLock = {
    /**
     * Enforces the display and zoom lock manually.
     * Safe across Android versions, tablets, and split-screen modes.
     *
     * @param {Function} [successCallback] - Called on successful application or safe fallback.
     * @param {Function} [errorCallback]   - Called if activity is invalid or an exception occurs.
     */
    enforceLock: function (successCallback, errorCallback) {
        var success = typeof successCallback === 'function' ? successCallback : function () {};
        var error = typeof errorCallback === 'function' ? errorCallback : function () {};

        exec(success, error, 'DisplayLock', 'enforceLock', []);
    }
};

module.exports = DisplayLock;
