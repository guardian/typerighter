import { EuiBasicTableColumn, EuiButton, EuiFlexGroup, EuiFlexItem, EuiIcon, EuiInMemoryTable, EuiInMemoryTableProps, EuiInlineEditText, EuiTitle, EuiToast, EuiToolTip } from "@elastic/eui"
import { css } from "@emotion/react"
import { Tag, useTags } from "./hooks/useTags";
import React, { useEffect, useState } from "react";
import styled from "@emotion/styled";
import { useCreateEditPermissions } from "./RulesTable";
import { update } from "lodash";


// const EditTag = ({
//     editIsEnabled,
//     editTag,
//     tag, 
//   }: { editIsEnabled: boolean, editTag: (ruleId: number) => void, tag: Tag }) => {
//     return <EuiToolTip
//             content={editIsEnabled ? "" : "You do not have the correct permissions to edit a Tag. Please contact Central Production if you need to edit tags."}>
//             <EditTagButton editIsEnabled={editIsEnabled}
//                 onClick={() => (editIsEnabled ? editTag(tag.id) : () => null)}>
//             <EuiIcon type="pencil"/>
//         </EditTagButton>
//     </EuiToolTip>
// }


type DeleteTagButtonProps = {
    editIsEnabled: boolean;
}

const DeleteTagButton = styled.button<DeleteTagButtonProps>(props => ({
    width: '16px',
    cursor: props.editIsEnabled ? 'pointer' : 'not-allowed',
}))

const editableNameWidth = '18rem';

const DeleteTag = ({editIsEnabled, tag, openDeleteTagDialogue}: {editIsEnabled: boolean, tag: Tag, openDeleteTagDialogue: (tag: Tag) => void}) => 
    <EuiToolTip
        content={editIsEnabled ? "" : "You do not have the correct permissions to delete a tag. Please contact Central Production if you need to do so."}>
        <DeleteTagButton editIsEnabled={editIsEnabled} onClick={() => editIsEnabled ? openDeleteTagDialogue(tag) : null}>
            <EuiIcon type="trash" color="danger"/>
        </DeleteTagButton>
    </EuiToolTip>

export const EditableNameField = ({
    editIsEnabled,
    editTag,
    tag,
    fetchTags,
    isLoading,
  }: { editIsEnabled: boolean, editTag: (tag: Tag) => void, tag: Tag, fetchTags: () => void, isLoading: boolean }) => {
    const [tagName, setTagName] = useState(tag.name);
    useEffect(() => {
        setTagName(tag.name)
    }, [tag])
    return (
      <>
        <EuiFlexGroup css={css`width: ${editableNameWidth}`}>
            {editIsEnabled ? 
                <EuiInlineEditText
                    inputAriaLabel="Edit text inline"
                    defaultValue={tagName}
                    size='s'
                    onSave={(newInlineEditValue: string) => {
                        const newTag = {id: tag.id, name: newInlineEditValue}
                        editTag(newTag)
                    }}
                    css={css`width: 100%; color: black`}
                /> : <EuiToolTip
                        content={editIsEnabled ? "" : "You do not have the correct permissions to edit a tag. Please contact Central Production if you need to do so."}>
                        <EuiFlexItem>{tagName}</EuiFlexItem>
                    </EuiToolTip>
            }
        </EuiFlexGroup>
      </>
    );
  };

const createTagTableColumns = (editTag: (tag: Tag) => void, fetchTags: () => void, isLoading: boolean, openDeleteTagDialogue: (tag: Tag) => void): Array<EuiBasicTableColumn<Tag>> => {
    const hasEditPermissions = useCreateEditPermissions();
    return [
        {
            field: 'id',
            name: 'Tag ID',
            width: '5rem',
            sortable: true
        },
        {
            field: 'name',
            name: 'Name',
            width: editableNameWidth,
            actions: [{
              name: 'Edit',
              render: (item, enabled) => <EditableNameField editIsEnabled={enabled} editTag={editTag} tag={item} fetchTags={fetchTags} isLoading={isLoading} key={item.id} />,
              enabled: () => hasEditPermissions,
            //   'data-test-subj': 'action-edit',
            }]
          },
        {
            field: 'ruleCount',
            width: '10rem',
            name: 'Associated rules',
            sortable: true
        },
        {
            name: 'Delete',
            width: '4rem',
            actions: [{
                name: 'Delete',
                enabled: () => hasEditPermissions,
                render: (item, enabled) => <DeleteTag editIsEnabled={enabled} tag={item} openDeleteTagDialogue={openDeleteTagDialogue}/>
            }]
        }
    ]
}

const deleteTagWarningCss = css`
    position: absolute;
    bottom: 20px;
    right: 20px;
    background-color: white;
    padding: 20px;
    z-index: 1;
    box-shadow: 2px 2px 10px rgba(0,0,0,0.4)
`;

const DeleteTagWarning = ({tag, setTagToDelete, deleteTag}: {tag: Tag, setTagToDelete: (tag: Tag | null) => void, deleteTag: (tag: Tag) => void}) => {
    const deleteAndClosePrompty = (tag: Tag) => { 
        deleteTag(tag);
        setTagToDelete(null);
    }
    return <EuiToast
        title="Confirm Tag delete action"
        color="danger"
        iconType="warning"
        onClose={() => {setTagToDelete(null)}}
        css={deleteTagWarningCss}
    >
    <p>
      Are you sure you want to delete the tag '{tag.name}'?
    <br/>
      This action is <strong>permanent</strong> and will remove the tag from all associated rules.
    </p>

    <EuiFlexGroup justifyContent="flexEnd" gutterSize="s">
      <EuiFlexItem grow={false}>
        <EuiButton onClick={() => deleteAndClosePrompty(tag)}size="s" color="danger">Delete the '{tag.name}' tag</EuiButton>
      </EuiFlexItem>
    </EuiFlexGroup>
  </EuiToast>
}
export const TagsTable = () => {
    const {tags, fetchTags, isLoading, tagRuleCounts, fetchTagRuleCounts, isLoadingTagRuleCounts, updateTag, deleteTag} = useTags();
    const items = Object.values(tags).map(tag => {
        return {id: tag.id, name: tag.name, ruleCount: tagRuleCounts ? tagRuleCounts.draft.find(tagRule => tagRule.tagId === tag.id)?.ruleCount : 0}
    })

    const [tagToDelete, setTagToDelete] = useState<Tag | null>(null)

    const openDeleteTagDialogue = (tag: Tag) => {
        setTagToDelete(tag)
    }

    const columns = createTagTableColumns(updateTag, fetchTags, isLoading, openDeleteTagDialogue)
      
    return (<>
        <EuiFlexGroup>
            <EuiFlexItem/>
            <EuiFlexItem>
                <EuiFlexGroup>
                    <EuiFlexItem grow={false} css={css`padding-bottom: 20px;`}>
                        <EuiTitle>
                            <h1>Tags</h1>
                        </EuiTitle>
                    </EuiFlexItem>
                </EuiFlexGroup>
                <EuiFlexGroup>
                    {tagToDelete ? <DeleteTagWarning tag={tagToDelete} setTagToDelete={setTagToDelete} deleteTag={deleteTag}/> : null}
                    <EuiFlexItem>
                        <EuiInMemoryTable
                            columns={columns}
                            items={items}
                            css={css`max-width: 500px`}
                            sorting={{sort: {
                                field: 'id',
                                direction: 'asc',
                            }}}
                        >
                        </EuiInMemoryTable>
                    </EuiFlexItem>
                </EuiFlexGroup>
            </ EuiFlexItem>
            <EuiFlexItem/>
        </EuiFlexGroup>
    </>)
}