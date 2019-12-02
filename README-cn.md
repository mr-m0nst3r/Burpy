# Burpy
一个可以让你能够在Burpsuite中运行自己指定python脚本的插件。

# Intro

写这个插件的原因是因为这样我可以在Burpsuite里面直接执行python，尤其是当需要对一些明文数据进行RSA加密之后再发送给服务器的时候。

正如前面所说，使用这个插件时，我们可以写一个python小脚本来进行RSA加密，并指定一个公钥，这样我们就可以直接在Burp里面得到加密之后的结果，可以省去在命令行／工具界面 和 Burp 界面复制粘贴的麻烦。

# Changelog

- 开始使用类，而不用一个函数，这样我们可以在加载webdriver + selenium的时候将这个类创建起来，每次调用的时候就不用重复 webdriver+selenium的初始化过程。（经过测试，这样实现起来没有卡顿的感觉；因为webdriver+selenium 比较费时。如果你测试过一些H5页面，并且其中数据通过一些js函数进行了签名或者加密，你就知道我说的是什么了。）
- added auto burpy call to do something for the whole body
- 增加`自动调用`按钮，处理对象是http body。（所以需要自己解析相应参数）

# Usage

1. 安装PyRO, version 4 is used.
2. 加载插件后配置参数
3. 指定你的python脚本
4. `spawn` 用来测试
5. 右键菜单中的`Burpy Call` 来调用你的脚本

# the python script sample

> 对与更加详细的用法，请参看`examples`文件夹中的内容。

下面是一个简易的base64编码的python脚本
```python
# Burpy插件会调用 `Burpy`类的`main`方法，只需要在这个方法中返回字符串就可以了。就这么简单。
class Burpy:
    def main(self,args):
        from base64 import b64encode
        return b64encode(args)
```

# Reference
the great Brida

# others
- 欢迎意见指导