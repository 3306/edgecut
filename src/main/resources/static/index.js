/**
 * Created by wanghuanyu on 2017/10/29.
 */

document.addEventListener('DOMContentLoaded',function (event) {
    let src = new URL(document.getElementById('modLoader').src);
    let url = '//'+src.host + src.pathname.replace(/\/index.js$/g,'/');
    function load(url,cb) {

        let script = document.createElement('script');
        script.src = url + '?t='+new Date().getTime() ;
        script.charset = 'utf-8';
        script.onload = function () {
            if(cb) cb(null);
        };
        script.onerror = function () {
            if(cb) cb(new Error(url))
        };
        document.body.appendChild(script);
    };
    function controllerLoader(BaseURL) {
        let className = document.body.className;
        if(!!className){
            load(BaseURL+className+'.js',function (error) {
                if(error) alert('加载文件失败,直接找管理员');
            })
        }
    }
    controllerLoader(url);
});