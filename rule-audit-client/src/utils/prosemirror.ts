import { EditorState } from "prosemirror-state";
import { EditorView } from "prosemirror-view";
import { Schema, DOMParser as ProsemirrorDOMParser } from "prosemirror-model";
import { marks, schema } from "prosemirror-schema-basic";
import { addListNodes } from "prosemirror-schema-list";
import { getBlocksFromDocument } from "@guardian/prosemirror-typerighter";

const appSchema = new Schema({
  nodes: addListNodes(schema.spec.nodes as any, "paragraph block*", "block"),
  marks
});

const prosemirrorParser = ProsemirrorDOMParser.fromSchema(appSchema);
const domParser = new DOMParser();

export const getBlocksFromHtmlString = (htmlStr: string) => {
  const xmlDoc = domParser.parseFromString(htmlStr, "application/xml");
  const doc = prosemirrorParser.parse(xmlDoc);
  return getBlocksFromDocument(doc);
};
