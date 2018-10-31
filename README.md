# Cookie Extractor

This Burp extension extracts all the values a particular cookie is set to, using data from the Proxy History.

To use it, right-click on a request that contains the cookie you're interested in, in the Proxy History table.
The pop-up context menu will includes *Extract Cookie Values* and a sub menu lets you pick the relevant cookie. The values
will be extracted from all the *Set-Cookie* headers and you will be prompted for a file name to save these into.