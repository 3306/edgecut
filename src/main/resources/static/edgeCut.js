(($)=>{

    let list = $('#list');
    let getId = (key)=>{
        return '_' + key.replace('.','').replace('/','');
    };
    let search = new URL(window.location.href).searchParams;
    let all = {};
    let listItemTemplate = (imgUrl,key)=>{
        return `    <div class="col-xs-6">
    <div >
      <img id="${getId(key)}" src="${imgUrl}">
      <div class="caption">
        <p><a data-originkey="${key}" data-key="${getId(key)}" class="alter-pic btn btn-primary" role="button">确认修改</a></p>
      </div>
    </div>
</div>
                `
    };

    let fetchPic = ()=>{
        $.ajax(
            {
                url:'//101.132.181.91/service/result',
                data:{
                    prefix:search.get('prefix') || "1",
                    count:search.get('count') || "100"
                },
                dataType:'jsonp',
                success:(res)=>{
                    // console.log (res.data);
                    let result = res.data.map((item)=>{
                        return listItemTemplate(item.originDownloadUrl+'?x-oss-process=image/format,jpg/quality,q_20',item.key);
                    }).join('');
                    // console.log (result);
                    // list.append(result)
                    list.html(result);
                    res.data.forEach((item)=>{
                        $(`#${getId(item.key)}`).Jcrop({
                            boxWidth:600
                        },function () {
                            // this.setSelect([100,100,200,150],function (a) {
                            //     console.log ('123132');
                            // })

                            this.setSelect([item.x,item.y,item.x+item.w,item.y+item.h]);
                            all[getId(item.key)] = this;
                        });

                    })
                }
            }
        )
    };

    let init = ()=>{
        fetchPic();
        $('body').on('click','.alter-pic',(e)=>{
            let key = e.target.dataset.key;
            let originKey = e.target.dataset.originkey;
            let current = all[key].tellSelect ();
            console.log (current);
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
                    console.log (res);
                    $.jGrowl('修改成功');
                }
            })
        });
    };

    init();
})(jQuery);