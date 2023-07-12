import { EuiBasicTableColumn, EuiButton, EuiFlexGroup, EuiFlexItem, EuiIcon, EuiInMemoryTable, EuiInMemoryTableProps, EuiInlineEditText, EuiTitle, EuiToast, EuiToolTip } from "@elastic/eui"
import { css } from "@emotion/react"
import { Tag, useTags } from "./hooks/useTags";
import React, { useState } from "react";
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

const DeleteTag = ({editIsEnabled, tag, openDeleteTagDialogue}: {editIsEnabled: boolean, tag: Tag, openDeleteTagDialogue: (tag: Tag) => void}) => {

    return <DeleteTagButton editIsEnabled={editIsEnabled} onClick={() => openDeleteTagDialogue(tag)}>
        <EuiIcon type="trash" color="danger"/>
        </DeleteTagButton>
}

export const EditableNameField = ({
    editIsEnabled,
    editTag,
    tag,
    fetchTags,
    isLoading,
  }: { editIsEnabled: boolean, editTag: (tag: Tag) => void, tag: Tag, fetchTags: () => void, isLoading: boolean }) => {
    console.log(tag)
    return (
      <>
        <EuiFlexGroup css={css`width: ${editableNameWidth}`}>
            <EuiInlineEditText
                inputAriaLabel="Edit text inline"
                defaultValue={tag.name}
                size='s'
                onSave={(newInlineEditValue: string) => {
                    console.log("WHU")
                    const newTag = {id: tag.id, name: newInlineEditValue}
                    editTag(newTag)
                }}
                css={css`width: 100%; color: black`}
            />
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
            width: '4rem'
        },
        {
            field: 'name',
            name: 'Name',
            width: editableNameWidth,
            actions: [{
              name: 'Edit',
              render: (item, enabled) => <EditableNameField editIsEnabled={enabled} editTag={editTag} tag={item} fetchTags={fetchTags} isLoading={isLoading} />,
              isPrimary: true,
              description: 'Edit this tag',
            //   onClick: (tag: Tag) => {
            //     editTag(tag.id)
            //   },
              enabled: () => hasEditPermissions,
              'data-test-subj': 'action-edit',
            }]
          },
        {
            field: 'ruleCount',
            width: '10rem',
            name: 'Associated rules',
        },
        {
            width: '4rem',
            name: 'Delete',
            actions: [{
                name: 'Delete',
                render: (item, enabled) => <DeleteTag editIsEnabled={enabled} tag={item} openDeleteTagDialogue={openDeleteTagDialogue}/>
            }]
        }
    ]
}

const DeleteTagWarning = ({tag, setTagToDelete, deleteTag}: {tag: Tag, setTagToDelete: (tag: Tag | null) => void, deleteTag: (tag: Tag) => void}) => {
    return <EuiToast
        title="Confirm Tag delete action"
        color="danger"
        iconType="warning"
        onClose={() => {setTagToDelete(null)}}
        css={css`
            position: absolute;
            bottom: 20px;
            right: 20px;
            background-color: white;
            padding: 20px;
            z-index: 1;
            box-shadow: 2px 2px 10px rgba(0,0,0,0.4)
        `}
    >
    <p>
      Are you sure you want to delete the tag '{tag.name}'?
    <br/>
      This action is <strong>permanent</strong> and will remove the tag from all associated rules.
    </p>

    <EuiFlexGroup justifyContent="flexEnd" gutterSize="s">
      <EuiFlexItem grow={false}>
        <EuiButton onClick={() => deleteTag(tag)}size="s" color="danger">Delete the '{tag.name}' tag</EuiButton>
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
        console.log(tag)
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