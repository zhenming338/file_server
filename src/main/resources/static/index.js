function updateNavigator() {
    doms.navigatorCon.innerHTML = ''
    const width = window.innerWidth
    pathInfo.pathList.forEach((item, index) => {
        const element = document.createElement('div')
        element.classList.add('navigator-item')
        element.textContent = item
        console.log(item)
        element.addEventListener('click', e => {
            const newPathList = pathInfo.pathList.slice(0, index + 1)
            pathInfo.pathList = newPathList
        })
        doms.navigatorCon.appendChild(element)
    });
}

function updateFileList() {
    doms.contentCon.innerHTML = ''
    const width = window.innerWidth
    pathInfo.fileList.forEach((item, index) => {
        let element;
        if (item.isFile) {
            element = document.createElement('a')
            pathParm = ''
            pathInfo.pathList.forEach((item, index) => {
                if (index != 0) {
                    pathParm += item + "/"
                }
            })
            pathParm += item.name
            element.href = "/file/download/" + pathParm
            element.classList.add('file')
        } else {
            element = document.createElement('div')
        }
        element.textContent = item.name
        element.classList.add('file-item')
        element.addEventListener('click', (e) => {
            if (item.isFile) {
                console.log("start to downlaod file")
            } else {
                pathInfo.pathList = [...pathInfo.pathList, item.name]
            }
        })
        doms.contentCon.appendChild(element)
    })
}
let pathInfo = {}
let pathListValue = new Proxy(['home'], {
    set(target, key, value) {
        target[key] = value;
        updateNavigator();
        return true;
    }
});

let fileListValue = new Proxy([{}], {
    set(target, key, value) {
        target[key] = value;
        return true;
    }
});
Object.defineProperty(pathInfo, 'pathList',
    {
        get() {

            console.log(pathListValue)
            return pathListValue;
        },
        set(newValue) {
            pathListValue = new Proxy(newValue, {
                set(target, key, value) {
                    target[key] = value;
                    updateNavigator();
                    getFileList()
                    return true;
                }
            });

            updateNavigator();
            getFileList()
        }
    }
)
Object.defineProperty(pathInfo, 'fileList', {
    get() {
        console.log()
        return fileListValue
    },
    set(newValue) {
        fileListValue = new Proxy(newValue, {
            set(target, key, value) {
                target[key] = value
                updateFileList();
                return true
            }
        })

        updateFileList()
    }
})
let doms = {
    mainCon: document.querySelector('.main-container'),
    navigatorCon: document.querySelector('.navigator-container'),
    contentCon: document.querySelector('.content-container')
}
updateNavigator()
// pathInfo.pathList.push('test')

function getFileList() {
    console.log("start to fetch")
    let param = ""
    pathInfo.pathList.forEach((item, index) => {
        if (index != 0) {
            param += item + "/"
        }
    })
    fetch('/api/getDirChildren' + "?path=" + param, {
        method: "get",
    }).then(response => response.json())
        .then(data => {
            if (data.code == 200) {
                const fileList = data.data.sort((a, b) => {
                    if (a.isFile === b.isFile) {
                        return 0
                    }
                    return a.isFile ? 1 : -1
                })
                pathInfo.fileList = fileList
            } else {
                alert(data.message)
            }
            console.log(data)
        })
        .catch(err => console.log("请求失败", err))
}

getFileList()

