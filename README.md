# Athento UI for Nuxeo #

## Save your contentView columns ##

- You can select and save your favourite columns. After logout and login, you will have your selected columns again.


## Preview without authentication ##

Preview is based on Restlet API with URL below:

GET /nuxeo/restAPI/athpreview/{repoId}/{docId}/{fieldPath}?subPath=(subPath)&amp;token=(token)

where:

- repoId: is the repository name.
- docId: is the document identifier.
- fieldPath: is the xpath of content to preview.
- subpath: is use to reference images into preview (with no access token control)
- token: is the basic control access token based on changedToken of dublincore:modified metadata.

## Select a page in a ContentView ##

- You can introduce a page index into page-navigator to select your page in a ContentView.


## Preview (pdf2htmlEX based) ##

### Prerequisites ###

You can find install and quickstart information here: https://github.com/coolwanglu/pdf2htmlEX/wiki/Quick-Start

Important about fonts:

- In MacOX: You need to set the config fonts with 

        export FONTCONFIG_PATH=/opt/X11/lib/X11/fontconfig
