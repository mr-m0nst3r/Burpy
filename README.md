# Burpy
A plugin that allows you execute python and get return to BurpSuite.

# Intro
The reason I wrote this plugin is that, this enables me to use python inside BurpSuite, especially when I have to use RSA to encrypt some plaintext and then send it to the server during pentest.

Using this plugin, as described above, we can write a python script to do the RSA encryption using public key, then directly get the encrypted result from within Burp, saving life from tons of `copy`ing and `paste`ing between console and Burp.

# Changelog
- change to use class instead of pure function, so that we can init webdriver+selenium when loading without init it per call
- added auto burpy call to do something for the whole body

# Usage
1. install PyRO, version 4 is used.
2. configure python and pyro settings
3. configure the python file you wanna run
4. use `spawn` to test the result
5. use `Burpy Call` context memu to invoke your script

# the python script sample
The following example is a base64 encode function
```python
# the Burpy will call Burpy.main method, so make sure to return strings for this method, it's just that simple
class Burpy:
    def main(self,args):
        from base64 import b64encode
        return b64encode(args)
```

# Reference
the great Brida

# others
- Good ideas and contributions are welcomed.