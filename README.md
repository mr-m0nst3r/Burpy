# Burpy
A plugin that allows you execute python and get return to BurpSuite.

# Intro
During Android APP pentesting, I found it very often that the traffic is encrypted and/or signed, it would be great to have a plugin so we can write python to enc/dec/sign.

And, sometimes, you may just want some customized function to modify part of the traffic, all you need is just `Burpy Main`.

If you wanna take advantage of the intruder with payloads need to be encrypted, you need to `Enable Processor`, and write your own payload processor function.

# Changelog
- change to use class instead of pure function, so that we can init webdriver+selenium when loading without init it per call
- modified plugin to enable 4 function calls: main/enc/dec/sign
- add payload processor

# Usage

> check the examples for scripts

1. install PyRO, version 4 is used.
2. configure python and pyro settings
3. configure the python file you wanna run
4. use `spawn` to test the result
5. use `Burpy Main`/`Burpy Enc`/`Burpy Dec`/`Burpy Sign` context memu to invoke your script
6. write own payload processor, especially usefull with enc/dec

# the python script sample
Just write your own logic to modify the header/body as your need, and return the header/body, just that simple!
Note: if you need to handle response data, e.g decrypt response, you need to check whether the `header[0]` is `RESPONSE` or not.
If the data burpsuite sent to python comes from `response`, the header will be `header[0] == 'RESPONSE'`
```python
class Burpy:
    '''
    header is list, append as your need
    body is string, modify as your need
    '''
    def main(self, header, body):
        return header, body
    
    def encrypt(self, header, body):
        header.append("Enc: AAA")
        return header, body

    def decrypt(self, header, body):
        '''
        We usually use decrypt in two situations:
        1- decrypt message that sent to the server
        2- decrypt server response

        So, it's necessary to make a check because the 2 situations are totally different in term of how burp should behave
        '''
        if(header[0] != 'RESPONSE'):
            # meaning the data is not from response, so we can set new http header and body
            # header = magic(header)
            # body = magic(body)
        else:
            # meaning the data comes from response, we can't and don't need to renew it, so we just display it using pop box
            # so you can just do your magic to body param
            # body = magic(body)
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