/* DailyPlanetH2 browser helpers. No inline JavaScript: compatible with CSP script-src 'self'. */
(function () {
    'use strict';

    function meta(name) {
        var element = document.querySelector('meta[name="' + name + '"]');
        return element ? element.getAttribute('content') : '';
    }

    window.dpCsrf = {
        header: meta('_csrf_header') || 'X-CSRF-TOKEN',
        token: meta('_csrf'),
        fetch: function (url, options) {
            options = options || {};
            options.method = options.method || 'POST';
            options.headers = options.headers || {};
            if (this.token) {
                options.headers[this.header] = this.token;
            }
            return window.fetch(url, options);
        }
    };
}());
