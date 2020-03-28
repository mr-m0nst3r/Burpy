class Burpy:
    def main(self, args):
        data = args
        print "Data:",data
        return(self.sign(data))

    def sign(self,data):
        from Crypto.PublicKey import RSA
        from base64 import b64decode,b64encode
        from Crypto.Cipher import PKCS1_v1_5
        pubStr = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCpT2L1LNlW3B/8xT/2eSRZHE9waDduhQLtVSTfaR9BHs5SlhB2HqB9fXQB+2dpLU5RWjRygHKeC2cV2tdux2q4op+Aea8NZjWPCKeraT2ZJKZfwWU0Inl9owBiLVvaAPds3FG2iSoSU26n8L1At135flAMLZ94STU7yWkLbcTKrwIDAQAB"
        msg = data
        #msg = "1565246122420" + msg
        keyDER = b64decode(pubStr)
        keyPub = RSA.importKey(keyDER)
        cipher = PKCS1_v1_5.new(keyPub)
        ct = cipher.encrypt(msg.encode('utf-8'))
        ect = b64encode(ct)
        return ect