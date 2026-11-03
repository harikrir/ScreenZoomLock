var exec = require('cordova/exec');

var DisplayLock = {
    /**
     * Enforces the display and zoom lock manually.
     * Safe across Android versions, tablets, and split-screen modes.
     *
     * @param {Function} [successCallback] - Callback for successful lock or safe fallback
     * @param {Function} [errorCallback]   - Callback if execution fails
     */
    enforceLock: function (successCallback, errorCallback) {
        var success = typeof successCallback === 'function' ? successCallback : function () {};
        var error = typeof errorCallback === 'function' ? errorCallback : function () {};

        exec(success, error, 'DisplayLock', 'enforceLock', []);
    }
};

module.exports = DisplayLock;
