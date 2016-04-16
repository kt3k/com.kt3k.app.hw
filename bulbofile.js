const bulbo = require('bulbo')
const asset = bulbo.asset
const frontMatter = require('gulp-front-matter')
const wrap = require('gulp-wrap')

bulbo.dest('assets')

// js
asset('src-assets/js/**/*').base('src-assets')

// images
asset('src-assets/images/**/*').base('src-assets')

// html
asset('src-assets/*.html')
.base('src-assets')
.pipe(frontMatter())
.pipe(wrap({src: 'src-assets/layout/layout.nunjucks'}, null, {engine: 'nunjucks'}))
