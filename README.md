# Burpy
A plugin that allows you execute python and get return to BurpSuite.

# Intro
During Android APP pentesting, I found it very often that the traffic is encrypted and/or signed, it would be great to have a plugin so we can write python to enc/dec/sign.

And, sometimes, you may just want some customized function to modify part of the traffic, all you need is just write a python script and directly call it from within burpsuite.

If you wanna take advantage of the intruder with payloads need to be encrypted, you need to `Enable Processor`, and write your own payload processor function.

# Author
m0nst3r(Song Xinlei)

# Contributors with love
- @Center-Sun
- @ViCrack


# TODO
- [x] add tab's in request and response, so we don't have to click our mouse that often (allow specify which method to call for response/request tab)
- [x] to python3, from version `1.3`
- [x] `dynamic` function transform
- [x] resize and context menu support for popups (@ViCrack)
- [ ] Syntax highlight for popups
- [x] word wrap for popups
- [ ] python venv support

# Changelog
- change to use class instead of pure function, so that we can init webdriver+selenium when loading without init it per call
- modified plugin to enable 4 function calls: main/enc/dec/sign
- add payload processor
- add auto enc/dec. encrypt function automatically called when you click GO in burp, and decrypt function automatically called when receive response
- changed default pyro4 port, avoiding brida conflicts
- migration to python3
- dynamic context menu items extracted from your python script
- add `first_line` variable to `header` dict
- remove py editor, because it seems quite useless, and may lead to file mis-overwritten.
- upgrade pyro4 to pyro5, due to a bug#80 of pyrolite
- add message tab to call `encrypt/decrypt` automatically

# Usage (`>=v2.0`)
> NOTE: MAKE SURE YOU HAVE ALL DEPENDENCIES INSTALLED, INCLUDING THE DEPENDENCIES NEEDED FOR YOUR PYTHON SCRIPT

1. ~~install PyRO, version 4 is used.~~
2. install Pyro, version 5 is used: `pip install pyro5`
3. configure python and pyro settings: **DON'T USE `LOCALHOST`, use `127.0.0.1` instead**
4. configure the python file you wanna run
5. click "Start server", burpy will read your python script file and get all functions to generate the context menu
6. use context memu item to invoke your script's regarding function
7. write own payload processor, especially usefull with enc/dec

> ~~Install editor plugin example: mvn install:install-file -DgroupId=com.fifesoft -DartifactId=rsyntaxtextarea -Dversion=2.6.1.edited -Dpackaging=jar -Dfile=/home/m0nst3r/study/java/rsyntaxtextarea-2.6.1.edited.jar~~
> not necessary after version 2.4.4, as py editor was removed.

# the python script sample
Just write your own logic to modify the header/body as your need, and return the header/body, just that simple!

All functions will be extracted to generate context menu, except thos with `_`, `__`prefix!

> Note: header["first_line"] ==> `GET /XXX/yyy.php?param1=hello HTTP/1.1`.

```python
class Burpy:
    '''
    header is dict
    body is string
    '''
    def __init__(self):
        '''
        here goes some code that will be kept since "start server" clicked, for example, webdriver, which usually takes long time to init
        '''
        pass
        
    def main(self, header, body):
        return header, body

    def _test(self, param):
        '''
        function with `_`, `__`as starting letter will be ignored for context menu

        '''
        # param = magic(param)
        return param
    
    def encrypt(self, header, body):
        '''
        Auto Enc/Dec feature require this function
        '''
        header["Cookie"] = "admin=1"
        return header, body

    def decrypt(self, header, body):
        '''
        Auto Enc/Dec feature require this function

        '''
        # header = magic(header)
        # body = magic(body)
        return header, body

    def processor(self, payload):
        '''
        Enable Processor feature require this function
        payload processor function
        '''
        return payload+"123"
```

# Usage (`<v2.0`)

> check the examples for scripts
> NOTE: MAKE SURE YOU HAVE ALL DEPENDENCIES INSTALLED, INCLUDING THE DEPENDENCIES NEEDED FOR YOUR PYTHON SCRIPT

1. install PyRO, version 4 is used.
2. configure python and pyro settings
3. configure the python file you wanna run
4. use `spawn` to test the result
5. use `Burpy Main`/`Burpy Enc`/`Burpy Dec`/`Burpy Sign` context memu to invoke your script
6. write own payload processor, especially usefull with enc/dec

> Install editor plugin example: mvn install:install-file -DgroupId=com.fifesoft -DartifactId=rsyntaxtextarea -Dversion=2.6.1.edited -Dpackaging=jar -Dfile=/home/m0nst3r/study/java/rsyntaxtextarea-2.6.1.edited.jar

# the python script sample
Just write your own logic to modify the header/body as your need, and return the header/body, just that simple!
Note: if you need to handle response data, e.g decrypt response, you may want to write if-else, because in some cases, the response is different with the request. For example, the request is `encrypted=XXXXXX`, but the response is `XXXXXX`, without `encrypted`. 
```python
class Burpy:
    '''
    header is dict
    body is string
    '''
    def __init__(self):
        '''
        here goes some code that will be kept since "start server" clicked, for example, webdriver, which usually takes long time to init
        '''
        pass
        
    def main(self, header, body):
        return header, body
    
    def encrypt(self, header, body):
        header["Cookie"] = "admin=1"
        return header, body

    def decrypt(self, header, body):
        '''
        You may want to add logic if the response differ from the request, for example in the request, the encrypted data is followed after "data=", but in the response, the whole response body is encrypted data, without "data="
        '''
        # header = magic(header)
        # body = magic(body)
        return header, body

    def sign(self, header, body):
        header.update({"Sign":"123123123"})
        return header, body

    def processor(self, payload):
        '''
        payload processor function
        '''
        return payload+"123"
```

# Reference
the great Brida

# others
- Good ideas and contributions are welcomed.
