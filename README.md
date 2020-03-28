# Burpy
A plugin that allows you execute python and get return to BurpSuite.

# Intro
During Android APP pentesting, I found it very often that the traffic is encrypted and/or signed, it would be great to have a plugin so we can write python to enc/dec/sign.

And, sometimes, you may just want some customized function to modify part of the traffic, or, generate payloads for your intruder, that when the `Burpy Main` comes for.

You can enable/disable Enc/Dec/Sign in the UI so you can focus on the `Burpy Main`
# Changelog
- change to use class instead of pure function, so that we can init webdriver+selenium when loading without init it per call
- added auto burpy call to do something for the whole body
- modified plugin to enable 4 function calls: main/enc/dec/sign

# Usage
1. install PyRO, version 4 is used.
2. configure python and pyro settings
3. configure the python file you wanna run
4. use `spawn` to test the result
5. use `Burpy Main`/`Burpy Enc`/`Burpy Dec`/`Burpy Sign` context memu to invoke your script
6. write own payload processor, especially usefull with enc/dec

# the python script sample
Just write your own logic to modify the header/body as your need, and return the header/body, just that simple!
```python
class Burpy:
    '''
    header is list, append as your need
    body is string, modify as your need
    '''
    def main(self, header, body):
        header.append("Main: AAA")
        print "head:", header
        print "body:", body
        return header, body
    
    def encrypt(self, header, body):
        header.append("Enc: AAA")
        return header, body

    def decrypt(self, header, body):
        header.append("Dec: AAA")
        return header, body

    def sign(self, header, body):
        header.append("Sign: AAA")
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