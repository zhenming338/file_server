function updateNavigator() {
    doms.navigatorCon.innerHTML = ''
    const width = window.innerWidth
    pathInfo.pathList.forEach((item, index) => {
        const element = document.createElement('div')
        element.classList.add('navigator-item')
        element.textContent = item
        console.log(item)
        element.addEventListener('click', e => {
            pathInfo.pathList = pathInfo.pathList.slice(0, index + 1)
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
            element = document.createElement('div')
            const iconElement = document.createElement('img')
            iconElement.src = '/icon/copy_icon.png'
            const linkElement = document.createElement('a')
            linkElement.textContent = item.name
            let pathParm = ''
            pathInfo.pathList.forEach((item, index) => {
                if (index !== 0) {
                    pathParm += item + "/"
                }
            })
            pathParm += item.name
            linkElement.href = "/file/download/" + pathParm
            iconElement.addEventListener('click', e => {
                navigator.clipboard
                    .writeText(
                        location.origin + "/file/download/" + pathParm
                    ).then(() => {
                    alert("copy success")
                }).catch((e) => {
                    alert(e)
                })
            })
            linkElement.classList.add('file-link')
            iconElement.classList.add('file-icon')
            element.classList.add('file')
            element.appendChild(linkElement)
            element.appendChild(iconElement)
        } else {
            element = document.createElement('div')
            element.textContent = item.name
            element.addEventListener('click', (e) => {
                if (item.isFile) {
                    console.log("start to download file")
                } else {
                    pathInfo.pathList = [...pathInfo.pathList, item.name]
                }
            })
        }
        element.classList.add('file-item')
        doms.contentCon.appendChild(element)
    })
}

let pathInfo = {
    "pathList": null
}
let modelInfo = {
    "modelState": false
}
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
let modelState = false;
Object.defineProperty(modelInfo, "modelState", {
    get() {
        return modelState;
    },
    set(newValue) {
        if (newValue) {
            // doms.uploadModel.classList.remove("hidden")
            // doms.pageCover.classList.remove("hidden")
            // const path = pathInfo.pathList.join("/")
            // const uploadTitleEle = document.querySelector(".upload-title")
            // uploadTitleEle.textContent = "上传文件到" + path
        } else {
            // doms.uploadModel.classList.add("hidden")
            // doms.pageCover.classList.add("hidden")
            // doms.uploadModel.reset()
        }
        modelState = newValue
    }
})
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
    headerCon: document.querySelector(".header-container"),
    systemCon: document.querySelector(".system-info-container"),
    mainCon: document.querySelector('.main-container'),
    navigatorCon: document.querySelector('.navigator-container'),
    contentCon: document.querySelector('.content-container'),
    // uploadBtn: document.querySelector(".upload-container"),
    // uploadModel: document.querySelector(".upload-form"),
    // pageCover: document.querySelector(".page-cover")
}

function getSystemInfo() {
    console.log("get systemInfo")
    fetch("/api/systemInfo", {method: "get"})
        .then(res => res.json())
        .then(res => {
            console.log(res)
            const saveNum = 0;
            const data = res.data
            const totalSpace = (data.totalSpace / 1024 / 1024 / 1024).toFixed(saveNum)
            const freeSpace = (data.freeSpace / 1024 / 1024 / 1024).toFixed(saveNum)
            const usedSpace = (totalSpace - freeSpace).toFixed(saveNum)

            const spaceInfoBorder = document.querySelector(".space-info-detail-container")
            const borderStyle = window.getComputedStyle(spaceInfoBorder)
            let borderWith = borderStyle.width
            borderWith = borderWith.slice(0, borderWith.length - 2)
            const root = document.documentElement
            const blockWidth = (usedSpace / totalSpace) * borderWith
            root.style.setProperty("--space-block-width", `${blockWidth}px`)
            const spaceInfoNum = ` ${usedSpace}/${totalSpace} GB`
            const spaceNumDiv = document.querySelector(".space-info-num-detail")
            spaceNumDiv.textContent = spaceInfoNum

            const systemNameDetail = document.querySelector(".system-name-detail")
            systemNameDetail.textContent = data.operationSystemName
        })
}

function getFileList() {
    console.log("start to fetch")
    let param = ""
    pathInfo.pathList.forEach((item, index) => {
        if (index !== 0) {
            param += item + "/"
        }
    })
    fetch('/api/getDirChildren' + "?path=" + param, {
        method: "get",
    }).then(response => response.json())
        .then(data => {
            if (data.code === 200) {
                pathInfo.fileList = data.data.sort((a, b) => {
                    if (a.isFile === b.isFile) {
                        return 0
                    }
                    return a.isFile ? 1 : -1
                })
            } else {
                alert(data.message)
            }
            console.log(data)
        })
        .catch(err => console.log("请求失败", err))
}

function hiddenModel() {
    const uploadFormEle = document.querySelector(".upload-form")
    uploadFormEle.classList.add("hidden")
}

function init() {
    // doms.uploadBtn.addEventListener("click", () => {
    //     modelInfo.modelState = !modelInfo.modelState
    // })
    // doms.pageCover.addEventListener("click", () => {
    //     modelInfo.modelState = false
    // })
    // doms.uploadModel.addEventListener("submit", async (e) => {
    //     e.preventDefault()
    //     const formData = new FormData(doms.uploadModel)
    //
    //     const path = "/file/upload/"
    //         + pathInfo.pathList.filter((_, index) => index !== 0).join("/")
    //     fetch(path, {
    //         method: "post",
    //         body: formData
    //     }).then(res => res.json()).then(data => {
    //         if (data.code === 200) {
    //             alert("upload success")
    //         } else {
    //             alert("error :" + data.message)
    //         }
    //     }).catch(e => {
    //
    //         alert(e)
    //     })
    //         .finally(() => {
    //             modelInfo.modelState = false
    //         })
    // })
}

window.onload = function () {
    init()
    updateNavigator()
    getSystemInfo()
    getFileList()
}

