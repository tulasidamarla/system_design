# Encoding


# Base64
- Computers usually store data as binary.
- Base64 is a text representation of data that consists of only 64 characters which 
  are the alphanumeric characters (lowercase and uppercase), +, /. 
  - These 64 characters are considered ‘safe’ because, they cannot be misinterpreted
    by legacy computers and programs with characters such as <, >, %, \n and many others.
- Base64 encoding is not meant to provide security.
- Base64 encoding is not meant to compress data.

# How it works

- Base64 RFC can be found at [Base64 RFC](https://www.rfc-editor.org/rfc/rfc4648).
- The Base64(2<sup>6</sup>) encoding is designed to represent arbitrary sequences of octets in a form that allows the use of both upper and lowercase letters but not required to be human-readable.
- Each character in base64 takes 6 bits(2<sup>6</sup>=64).   
- The encoding process represents 24-bit groups of input bits as output strings of 4 encoded characters.  
  - A 24-bit input group is formed by concatenating 3 8-bit input groups.
- These 24 bits are then treated as 4 concatenated 6-bit groups, each of which is translated into a single character in the base 64
  alphabet.
- The Base 64 Alphabet table can be seen below.


         Value Encoding  Value Encoding  Value Encoding  Value Encoding
             0 A            17 R            34 i            51 z
             1 B            18 S            35 j            52 0
             2 C            19 T            36 k            53 1
             3 D            20 U            37 l            54 2
             4 E            21 V            38 m            55 3
             5 F            22 W            39 n            56 4
             6 G            23 X            40 o            57 5
             7 H            24 Y            41 p            58 6
             8 I            25 Z            42 q            59 7
             9 J            26 a            43 r            60 8
            10 K            27 b            44 s            61 9
            11 L            28 c            45 t            62 +
            12 M            29 d            46 u            63 /
            13 N            30 e            47 v
            14 O            31 f            48 w         (pad) =
            15 P            32 g            49 x
            16 Q            33 h            50 y

## Special processing

- When fewer than 24 input bits are available in an input group, zero bits are added
    (on the right) to form an integral number of 6-bit groups.
- Padding at the end of the data is performed using the '=' character.
  - If final quantum of encoding input is an integral multiple of 24 bits, then the final unit of encoded output 
    will be an integral multiple of 4 characters without "=" padding.
  - If the final quantum of encoding input is exactly 8 bits, then the final unit of encoded output will be two 
    characters followed by two "=" padding characters.
  - If the final quantum of encoding input is exactly 16 bits; here, the final unit of encoded output will be three 
    characters followed by one "=" padding character.
- Example:    
  - `a` ASCII value is 97. It's binary value is `01100001`.
    - Split the above binary by 6 chars, append 4 0's for the second set, two `=` for the remaining as per the spec.
    - 011000 010000 ==
    - 24 16 ==  
    - The above values as per the table become    
    -->  YQ==

## Uses of Base64

- Base64 very useful when transferring files as text. 
   - Read the file's bytes and encode them to base64. 
   - Transmit the base64 string 
   - On the receiving side decode the string.
- This procedure is used when sending attachments over SMTP during emailing.
- Base64 is used commonly in a number of applications including email via MIME, as well as storing complex data in XML
  or JSON.
### Example:
- Suppose you want to embed couple of images within an XML document. The images are binary data, while the XML 
    document is text. As we know XML cannot handle embedded binary data or text data with characters like `<,>` etc.
- A typical XML document with embedded binary attachment like images as:    
```
<images>
    <image name="Sally">{binary gibberish that breaks XML parsers}</image>
    <image name="Bobby">{binary gibberish that breaks XML parsers}</image>
</images>
```
    
- Convert this into Base64 such that XML parses can handle this as:
    
```
<images>
  <image name="Sally" encoding="base64">j23894uaiAJSD3234kljasjkSD...</image>
  <image name="Bobby" encoding="base64">Ja3k23JKasil3452AsdfjlksKsasKD...</image>
</images>
```
## How to perform base64 encoding/decoding

- Conversion from bytes to base64 text is called encoding.
  - This is a bit different from other encoding/decoding types.
- Conversion from base64 text to bytes is called decoding.  
  
- Bash has a built-in command for base64 encoding/decoding. 
  - To encode to base64: `echo 'hello' | base64`.
  - To decode base64-encoded text to normal text: `echo 'aGVsbG8K' | base64 -d`.

- Node.js also has support for base64. Here is a class that you can use:

```
/**
* Attachment class.
* Converts base64 string to file and file to base64 string
* Converting a Buffer to a string is known as decoding.
* Converting a string to a Buffer is known as encoding.
* See: https://nodejs.org/api/buffer.html
*
* For binary to text, the naming convention is reversed.
* Converting Buffer to string is encoding.
* Converting string to Buffer is decoding.
*
*/
class Attachment {
constructor(){

    }

    /**
     * 
     * @param {string} base64Str 
     * @returns {Buffer} file buffer
     */
    static base64ToBuffer(base64Str) {
        const fileBuffer = Buffer.from(base64Str, 'base64');
        // console.log(fileBuffer)
        return fileBuffer;
    }

    /**
     * 
     * @param {Buffer} fileBuffer 
     * @returns { string } base64 encoded content
     */
    static bufferToBase64(fileBuffer) {
        const base64Encoded = fileBuffer.toString('base64')
        // console.log(base64Encoded)
        return base64Encoded
    }
}

You get the file buffer like so:

const fileBuffer = fs.readFileSync(path);
Or like so:

const buf = Buffer.from('hey there');
```

- We can use an API to do encoding and decoding. For ex:
  - POST https://mk34rgwhnf.execute-api.ap-south-1.amazonaws.com/base64-encode
  - POST https://mk34rgwhnf.execute-api.ap-south-1.amazonaws.com/base64-decode

<img src="../images/base64_aws_api.png" height=300 width=400>

### Fantasy example

- Suppose you are a spy and you're on a mission to copy and take back a picture of great value back to your country's 
  intelligence.
- This picture is on a computer that has no access to internet and no printer. All you have in your hands is a 
  pen and a single sheet of paper. No flash disk, no CD etc. What do you do?
- Your first option would be to convert the picture into binary 1s and 0s , copy those 1s and 0s to the paper one by 
  one and then run for it.
- However, this can be a challenge because representing a picture using only 1s and 0s as your alphabet will 
  result in very many 1s and 0s. Your paper is small and you don't have time. Plus, the more 1s and 0s the more 
  chances of error.
- Your second option is to use hexadecimal instead of binary. Hexadecimal allows for 16 instead of 2 possible 
  characters so you have a wider alphabet hence less paper and time required.
- A better option is to convert the picture into base64 and take advantage of yet another larger character set to 
  represent the data. Less paper and less time to complete.

