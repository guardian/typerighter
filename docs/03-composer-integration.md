# Integration with a local composer

You will need dev nginx and a local composer.

Follow the instructions in flexible-content/readme/03-running.md to get your local composer up.

It should be accessible at `https://composer.local.dev-gutools.co.uk/`.

Run a local typerighter service (see ./running-locally.md)

Edit /etc/gu/flexible-composerbackend.properties and set 
`typerighter.url=http://localhost:9000`

## Turning on typerighter and proving it works

"reStart" composer.

 * Create an article and edit it. 
 * Press shift-F12 to bring up the feature switch menu.
 * Turn on 'typerighter'
 * Reload article page.
 * You will now see the box-out for typeerighter features in the top right of the article.
 * Click "advanced", then "refresh".

Add the following text:

"Diane Abbott and Kier Starmer have been re-elected"

Click "Check whole document".  You should see "Diane Abbott" ticked with green underline.
You should not see "Kier Starmer" ticked.  This is because his first name is spelt "Keir"!

Correct this, and click "Check whole document" again.  Both names should now be ticked with green underline.

## Developing new typerighter rules.

If you wish to test new rules, you should visit the google doc (see [here](./01-google-sheet.md)) and edit appropriately
Once you have added a rule to the doc, you must either 
 * restart the local typerighter service
 * POST to /refresh on the local typerighter service 