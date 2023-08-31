# Integration with a local composer

You will need dev nginx and a local composer.

Follow the instructions in flexible-content/readme/03-running.md to get your local composer up.

It should be accessible at `https://composer.local.dev-gutools.co.uk/`.

Run a local typerighter service (see instructions for [running locally](./01-running-locally.md)).

Edit /.gu/flexible-composerbackend.properties and set
`typerighter.url=https://checker.typerighter.local.dev-gutools.co.uk`

## Running typerighter checker

"reStart" composer.

- Create an article and edit it
- Open the typerighter sidebar by clicking the button on the right
- Add the following text:

"Kier Starmer and Rishi Sunak debate live on TV"

- Click "Check document".
  - "Rishi Sunak" should have a green underline.
  - "Kier Starmer" should not have a green underline. This is because his first name is spelt "Keir"!

Correct this, and click "Check document" again. Both names should now have a green underline.

## Adding new typerighter rules

If you wish to test new rules, you can add and edit rules in the [typerighter rule manager](https://manager.typerighter.local.dev-gutools.co.uk/).
