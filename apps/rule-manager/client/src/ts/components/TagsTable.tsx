import { EuiBasicTableColumn, EuiButton, EuiFieldText, EuiFlexGroup, EuiFlexItem, EuiForm, EuiFormRow, EuiIcon, EuiInMemoryTable, EuiInMemoryTableProps, EuiInlineEditText, EuiSpacer, EuiText, EuiTextColor, EuiTitle, EuiToast, EuiToolTip } from "@elastic/eui"
import { css } from "@emotion/react"
import { Tag, useTags } from "./hooks/useTags";
import React, { useEffect, useState } from "react";
import styled from "@emotion/styled";
import { useCreateEditPermissions } from "./RulesTable";
import { create, set, update } from "lodash";
import { RuleFormSection } from "./RuleFormSection";
import { hasCreateEditPermissions } from "./helpers/hasCreateEditPermissions";


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
        <EuiFlexGroup css={css`width: ${editableNameWidth}; padding-left: 1.2rem;`}>
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

const createTagTableColumns = (editTag: (tag: Tag) => void, fetchTags: () => void, isLoading: boolean, openDeleteTagDialogue: (tag: Tag) => void, hasEditPermissions: boolean): Array<EuiBasicTableColumn<Tag>> => {
    return [
        {
            // Bit of a hack to sort alphabetically on load
            field: 'name',
            width: '0rem',
            name: 'Name',
            render: (item, enabled) => null,
            dataType: 'string',
            sortable: ({ name }) => name.toLowerCase(),
        },
        {
            name: 'Name',
            width: editableNameWidth,
            actions: [{
              name: 'Edit',
              render: (item, enabled) => <EditableNameField editIsEnabled={enabled} editTag={editTag} tag={item} fetchTags={fetchTags} isLoading={isLoading} key={item.id} />,
              enabled: () => hasEditPermissions,
            //   'data-test-subj': 'action-edit',
            }],
          },
        {
            field: 'ruleCount',
            width: '10rem',
            name: 'Associated rules',
            render: (value: number) => !!value ? value : 0,
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

const CreateTagForm = ({createTag, enabled}: {createTag: (tagName: string) => Promise<void>, enabled: boolean}) => {
    const [tagName, setTagName] = useState('');
    const [clientSideValidationError, setClientSideValidationError] = useState<string | null>(null);
    const createTagIfSuitable = (tagName: string) => {
        if (!tagName){
            setClientSideValidationError("A tag must contain text.")
        } else {
            createTag(tagName)
            setTagName('');
        }
    }
    return <><EuiFlexGroup css={css`margin-bottom: 1rem; width: 100%;`}><RuleFormSection title="CREATE NEW TAG">
        <EuiSpacer size="s" />
        <EuiFlexGroup css={css`width: 100%; display: flex; gap: 0.8rem;`}>
            <EuiFlexItem grow={true}>
                <EuiFieldText 
                    placeholder="Tag text..." 
                    value={tagName} 
                    onChange={(e) => {setClientSideValidationError(null); setTagName(e.target.value || "")}}
                    css={css`width: 100%; max-width: 100%;`}
                />
            </EuiFlexItem>
            <EuiFlexItem grow={false}>
                <EuiButton onClick={() => createTagIfSuitable(tagName)} color={"primary"} fill disabled={!enabled}>
                    Create Tag
                </EuiButton>
            </EuiFlexItem>
        </EuiFlexGroup>
        <EuiFlexGroup >
            <EuiTextColor color="danger">
                {clientSideValidationError ? <EuiText css={css`margin-top: 0.5rem;`}>{clientSideValidationError}</EuiText> : null}
            </EuiTextColor>
        </EuiFlexGroup>
    </RuleFormSection>
    </EuiFlexGroup>
    </>
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
    const {tags, fetchTags, isLoading, tagRuleCounts, fetchTagRuleCounts, isLoadingTagRuleCounts, updateTag, deleteTag, createTag} = useTags();
    const items = Object.values(tags).map(tag => {
        return {id: tag.id, name: tag.name, ruleCount: tagRuleCounts ? tagRuleCounts.draft.find(tagRule => tagRule.tagId === tag.id)?.ruleCount : 0}
    })

    const [tagToDelete, setTagToDelete] = useState<Tag | null>(null)

    const openDeleteTagDialogue = (tag: Tag) => {
        setTagToDelete(tag)
    }
    const hasEditPermissions = useCreateEditPermissions();

    const columns = createTagTableColumns(updateTag, fetchTags, isLoading, openDeleteTagDialogue, hasEditPermissions)

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
                    <CreateTagForm createTag={createTag} enabled={hasEditPermissions} />
                </EuiFlexGroup>
                <EuiFlexGroup>
                    {tagToDelete ? <DeleteTagWarning tag={tagToDelete} setTagToDelete={setTagToDelete} deleteTag={deleteTag}/> : null}
                    <EuiFlexItem>
                        <EuiInMemoryTable
                            columns={columns}
                            items={items}
                            sorting={{sort: {
                                field: 'name',
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