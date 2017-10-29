(($)=>{

    let list = $('#list');
    let fixedList = $('#fixedList');
    let getId = (key)=>{
        return '_' + key.replace('.','').replace('/','');
    };
    let search = new URL(window.location.href).searchParams;
    let all = {};
    let listItemTemplate = (imgUrl,key)=>{
        return `    <div class="col-xs-6">
    <div class="my-thumbnail">
      <img id="${getId(key)}" src="${imgUrl}" alt="123121323">
      <div class="caption">
        <a data-originkey="${key}" data-key="${getId(key)}" class="alter-pic btn btn-primary btn-block" role="button">确认修改</a>
      </div>
    </div>
</div>
                `
    };

    let fetchPic = ()=>{
        return new Promise((resolve)=>{
            $.ajax(
                {
                    url:'//101.132.181.91/service/result',
                    data:{
                        prefix:search.get('prefix') || "1",
                        count:search.get('count') || "10"
                    },
                    dataType:'jsonp',
                    success:(res)=>{
                        // console.log (res.data);
                        let result = res.data.filter((item)=>{
                            return item.status === '0';
                        }).map((item)=>{
                            return listItemTemplate(item.originDownloadUrl+'?x-oss-process=image/format,jpg/quality,q_20',item.key);
                        }).join('');
                        let fixedResult = res.data.filter((item)=>{
                            return item.status === '1';
                        }).map((item)=>{
                            return listItemTemplate(item.originDownloadUrl+'?x-oss-process=image/format,jpg/quality,q_20',item.key);
                        })
                        // console.log (result);
                        // list.append(result)
                        list.html(result);
                        fixedList.html(fixedResult);
                        res.data.forEach((item)=>{
                            $(`#${getId(item.key)}`).Jcrop({
                                boxWidth:window.screen.width/2 -20,
                                allowSelect:false
                            },function () {
                                // this.setSelect([100,100,200,150],function (a) {
                                //     console.log ('123132');
                                // })

                                this.setSelect([item.x,item.y,item.x+item.w,item.y+item.h]);
                                all[getId(item.key)] = this;
                            });

                        });
                        resolve(res);
                    }
                }
            )
        })
    };


    fetchPic().then((res)=>{

        $('body').on('click','.alter-pic',(e)=>{
            let key = e.target.dataset.key;
            let originKey = e.target.dataset.originkey;
            let current = all[key].tellSelect ();
            $.ajax({
                url:'http://101.132.181.91/service/update',
                data:{
                    key:originKey,
                    x:Math.round(current.x),
                    y:Math.round(current.y),
                    w:Math.round(current.w),
                    h:Math.round(current.h)
                },
                dataType:'jsonp',
                success:(res)=>{
                    $.jGrowl('修改成功');
                    // console.log($(e.target).parents('.col-xs-4'));

                    if ($(e.target).parents('#home').length === 1) {
                        $('body').find('#profile #fixedList').append(`<div class="col-xs-6">
                        ${$(e.target).parents('.col-xs-6').html()}
                        </div>`);
                        $(e.target).parents('.col-xs-6').remove();
                    }
                }
            })
        });
    });
})(jQuery);