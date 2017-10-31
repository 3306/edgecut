(($)=>{
    // let host = "";
    let host = "//101.132.181.91";
    let list = $('#list');
    let fixedList = $('#fixedList');
    let getId = (key)=>{
        return '_' + key.replace('.','').replace('/','');
    };
    let search = new URL(window.location.href).searchParams;
    let prefix = search.get('prefix');
    let listCurrentPage = 1;
    let fixedListCurrentPage = 1;
    let allCount = undefined;
    let pageSize = 5;
    let all = {};
    let listItemTemplate = (imgUrl,key)=>{
        return `    <div class="col-xs-12">
    <div class="my-thumbnail">
      <img id="${getId(key)}" src="${imgUrl}" alt="123121323">
      <div class="caption">
        <a data-originkey="${key}" data-key="${getId(key)}" class="alter-pic btn btn-primary btn-block" role="button">确认修改</a>
      </div>
    </div>
</div>
                `
    };

    let fetchPic = (prefix,currentPage,status = 1)=>{
        return new Promise((resolve)=>{
            $.ajax(
                {
                    url: host + '/service/result',
                    data:{
                        prefix:prefix || "1",
                        pageSize,
                        currentPage,
                        status
                    },
                    dataType:'jsonp',
                    success:(res)=>{
                        // console.log (res.data);
                        allCount = res.count;
                        let result = res.data.map((item)=>{
                            return listItemTemplate(item.originDownloadUrl+'?x-oss-process=image/format,jpg/quality,q_20',item.key);
                        })
                        // console.log (result);
                        // list.append(result)
                        if(status === 1){
                            list.html(result);
                        }else{
                            fixedList.html(result);
                        }

                        res.data.forEach((item)=>{
                            $(`#${getId(item.key)}`).Jcrop({
                                boxHeight:window.screen.height - 300,
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

    fetchPic(prefix,fixedListCurrentPage,2);
    fetchPic(prefix,listCurrentPage,1);

    $('body').on('click','.alter-pic',(e)=>{
        let key = e.target.dataset.key;
        let originKey = e.target.dataset.originkey;
        let current = all[key].tellSelect ();
        $.ajax({
            url: host + '/service/update',
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
                    if (list.children().length < 3) {
                        fetchPic(prefix,listCurrentPage,1);
                    }else{
                        $(e.target).parents('.col-xs-12').remove();
                    }
                }
            }
        })
    });
    $('body').on('click','.pager a',(e)=>{
        let action = e.target.dataset.action;
        action === 'next' ? ++fixedListCurrentPage : --fixedListCurrentPage
        if(fixedListCurrentPage < 1){
            fixedListCurrentPage = 1;
            return $.jGrowl('没有上一页了');
        }else if(fixedListCurrentPage > Math.ceil(allCount/pageSize)){
            return $.jGrowl('没有下一页了');
            --fixedListCurrentPage;
        }else{
            fetchPic(prefix,fixedListCurrentPage,2)
        }

    })
})(jQuery);