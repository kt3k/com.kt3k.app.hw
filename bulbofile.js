const bulbo = require('bulbo')
const asset = bulbo.asset

bulbo.dest('assets')

asset('src-assets/**/*')


