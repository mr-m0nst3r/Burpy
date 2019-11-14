#coding:utf-8

from selenium import webdriver
import json
from base64 import b64encode

chromeExec = "/usr/local/bin/chromedriver"
url = "https://xxxx/jn/index.html"

class Burpy:
    def __init__(self):
        """
        this is called from the start of PyRo4 service, so init webdriver here
        """
        option = webdriver.ChromeOptions()
        option.add_argument('headless')
        self.driver = webdriver.Chrome(executable_path=chromeExec, chrome_options=option)
        self.driver.get(url)

        js = self.load_js()
        self.driver.execute_script(js) 

    def load_js(self):
        jsFilePath = "/xxxx/moduleraid.js"
        jsContent = ""
        with open(jsFilePath) as f:
            jsContent = f.read()
        return jsContent

    def sign(self,data):

        data_json = json.loads(data) # {"data":{"qryType":"cpfl"},"sign":"LDr9iNugYLEAEaCj3Va60SatmA95/Xj+OSsHHvxORJbCwBtbtffm3BN9RbyRVZ6KgiZXmC1UMk+T+26+A0HbcW7AhIcXkQjYR8oZVrtA0QulXHzvGHK5qRyoGXDd5wcTM5xX+t8vlqGchhr1dq09d82L02KpZe/HNrtFoSr+ovwuPZAaCeEyfb68g1GBzUr67mHlOcZBMONlhmoru2Q5JrrSGJ9vjnpYdZqLMtiHagP8movvE6SpHpd+F7fDdWj7yUHtELxnHJeHQylPhsVWD/r3qEgPGlZKrobj4D53z85DKIeDYMfsBBwBdmv4iNVP6OkmXQSNvipGZxBREepWuw=="}

        part_data = data_json.get("data")
        #part_key = part_data.keys()[0]
        #part_value = part_data[part_key]

        getSign = """var a = %s; return JSON.stringify(window.mR.modules["1eef"].default.getSendSign(a))""" % (json.dumps(part_data))
        print "getSign: ", getSign

        result = self.driver.execute_script(getSign)
        # driver.quit()
        return(result)

    def down(self):
        self.driver.quit()

    def main(self, args):
        data = args
        print "Data:",data
        return(self.sign(data))

if __name__ == "__main__":
    data = ['{"data":{"qryType":"cpfl"},"sign":"LDr9iNugYLEAEaCj3Va60SatmA95/Xj+OSsHHvxORJbCwBtbtffm3BN9RbyRVZ6KgiZXmC1UMk+T+26+A0HbcW7AhIcXkQjYR8oZVrtA0QulXHzvGHK5qRyoGXDd5wcTM5xX+t8vlqGchhr1dq09d82L02KpZe/HNrtFoSr+ovwuPZAaCeEyfb68g1GBzUr67mHlOcZBMONlhmoru2Q5JrrSGJ9vjnpYdZqLMtiHagP8movvE6SpHpd+F7fDdWj7yUHtELxnHJeHQylPhsVWD/r3qEgPGlZKrobj4D53z85DKIeDYMfsBBwBdmv4iNVP6OkmXQSNvipGZxBREepWuw=="}']
    test = Burpy()
    print test.main(data)