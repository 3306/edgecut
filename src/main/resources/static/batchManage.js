(($) => {
    let host = "//101.132.181.91";
    let tbody = $('#mainTable');
    $.ajax({
        url: host + '/service/baseDir',
        dataType: 'jsonp',
        success: (res) => {
            res.map((item) => {
                let prefix = item.substr(0, item.length - 1);
                fetchDetail(prefix).then((data) => {
                    console.log(data);
                    let fixed = data['step-2'] || 0;
                    let unFixed = data['step-1'] || 0;
                    let neverRun = data['step-0'] || 0;
                    let all = fixed + unFixed + neverRun;
                    // data.data.forEach((jtem)=>{
                    //     if (jtem.status === '0'){
                    //         unFixed.push(jtem);
                    //     }else if(jtem.status === '1'){
                    //         fixed.push(item);
                    //     }else if(jtem.status === null){
                    //         neverRun.push(jtem)
                    //     }
                    // });
                    let result = `<tr>
                        <td>
                            <a href="./edgeCut.html?prefix=${prefix}&count=1000">${item}</a>
                        </td>
                        <td>
                            ${all}
                        </td>
                        <td>
                            ${neverRun}
                        </td><td>
                            ${unFixed}
                        </td><td>
                            ${fixed}
                        </td>
                        <td>
                            ${neverRun > 0 ? `<a class="btn btn-default run" data-key="${item.substr(0, item.length - 1)}">运行</a>` : ''}
                            ${fixed === all ? `<a  class="btn btn-default download" data-loading-text="正在打包中..." data-key="${prefix}">下载!</a>` : ''}
                        </td>
                    </tr>`;
                    tbody.append(result);
                });
            });
        }
    });

    function fetchDetail(prefix) {
        return $.ajax({
            url: host + '/service/dirCount',
            data: {
                prefix: prefix
            },
            dataType: 'jsonp',
            success: (res) => {
                return res;
            }
        });
    }

    // function download(prefix) {
    //     return $.ajax({
    //         url:host+'/download',
    //         dataType:'jsonp'
    //     })
    // }

    $('body').on('click', '.run', (e) => {
        let prefix = e.target.dataset.key;
        $.ajax({
            url: host + '/service/run',
            data: {
                prefix
            },
            dataType: 'jsonp',
            success: (res) => {
                if(res.over){
                    $.jGrowl('运行完成');
                    window.location.href = window.location.href;
                }else{
                    $.jGrowl('运行中')
                }
            }
        })
    });

    $('body').on('click','.download',(e)=>{
        var $btn = $(e.target).button('loading')
        // business logic...

        let prefix = e.target.dataset.key;
        $.ajax({
            url: host + '/service/download',
            data: {
                prefix
            },
            dataType: 'jsonp',
            success: (res) => {
                console.log(res);
                fetchDownloadStatus(res.targetUrl).then((download)=>{
                    console.log(download);
                    window.open(download.targetUrl);
                    $btn.button('reset')
                });
            }
        })
    });

    function fetchDownloadStatus(file) {
        function fetch(file,resolve) {
            $.ajax({
                url:host+'/service/downloadStatus',
                data:{
                    key:file
                },
                dataType:'jsonp',
                success:(res)=>{
                    if(res.closed){
                        resolve(res);
                    }else{
                        setTimeout(()=>{
                            fetch(file,resolve)
                        },2000)
                    }
                }
            })
        }
        return new Promise((resolve)=>{
            console.log(resolve);
            fetch(file,resolve);
        });
    }

    window.onload = function () {

        var OSS = window.OSS;
        if (OSS) {
            var appServer = host + '/service/stsUpload';
            var bucket = 'edgecut';
            var region = 'oss-cn-shanghai';

            var urllib = OSS.urllib;
            var Buffer = OSS.Buffer;
            var OSS = OSS.Wrapper;
            var STS = OSS.STS;


            // var client = new OSS({
            //     region: 'oss-cn-shanghai',
            //     accessKeyId: 'STS.LF67MBS9sqLvHySNMdAorh81j',
            //     accessKeySecret: '5J8mu8zvME4XbpDevGfo2TnHA27L1gq1GpFMeTe3hDTA',
            //     stsToken:'CAIS7gF1q6Ft5B2yfSjIrYWDfPf2vuZSxo6dSl/iqk0xTeBeh/2aiDz2IH1OeHVrCOoZvvwwm2pS6voZlqB6T55OSAmcNZIoElPqbp/iMeT7oMWQweEuqv/MQBq+aXPS2MvVfJ+KLrf0ceusbFbpjzJ6xaCAGxypQ12iN+/i6/clFKN1ODO1dj1bHtxbCxJ/ocsBTxvrOO2qLwThjxi7biMqmHIl1zogsP3gmp3Et0eG1w2n8IJP+dSteKrDRtJ3IZJyX+2y2OFLbafb2EZSkUMXrvsq1/0epmef4Y7HWQUNuA/oNPHP+9lrPJbmmwn0qykuGoABkk9yL79ULcJkEzrnsYBwpj7SoIh1k615sPzgIullgjdIaLJfd6VPztHVQNFDL5BL+pCgkjXpJ1nF9Kbh0VtE8eUOPcotQPiHyEB4d7QFsKl6gCIbH8Axe2EhSF3wwOSUEycYkeOqyKR0cq3E7la1UjocK/wFaBtSKhLf/sF29ZY=',
            //     bucket: 'edgecut'
            // });
            //
            // var applyTokenDo = function (func) {
            //     return func(client);
            // };

            var applyTokenDo = function (func) {
                var url = appServer;
                return new Promise((resolve) => {
                    $.ajax({
                        url,
                        dataType: 'jsonp',
                        success: (res) => {
                            resolve(res);
                        }
                    })
                }).then(function (result) {
                    var creds = result;
                    var client = new OSS({
                        region: region,
                        accessKeyId: creds.accessKeyId,
                        accessKeySecret: creds.accessKeySecret,
                        stsToken: creds.securityToken,
                        bucket: bucket
                    });
                    console.log(client);
                    return func(client);
                });
            };

            var progress = function (p) {
                return function (done) {
                    var bar = document.getElementById('progress-bar');
                    bar.style.width = Math.floor(p * 100) + '%';
                    bar.innerHTML = Math.floor(p * 100) + '%';
                    done();
                }
            };

            var uploadFile = function (client) {
                let successCount = 0;
                let files = document.getElementById('file').files;
                $('#allProgress').show();
                $('#itemProgress').show();
                $('#all').html(files.length);
                $('#current').html(1)
                // client.multipartUpload(key, files[i], {
                //     progress: progress
                // }).then(function (res) {
                //     $('#current').html(++successCount);
                //     console.log('upload success: %j', res);
                // });
                upload(client,files,0);

            };
            function upload(client,files,i) {
                if(i === files.length){
                    $.jGrowl('上传成功');
                    return $('#current').html(i);
                }else{
                    let key = files[i].webkitRelativePath;
                    client.multipartUpload(key, files[i], {
                        progress: progress
                    }).then(function (res) {
                        $('#current').html(i+1);
                        console.log('upload success: %j', res);
                        upload(client,files,++i);

                    });
                }

            }

            $('body').on('click', '#file-button', () => {
                applyTokenDo(uploadFile);
            })
        }
    }
})(jQuery)