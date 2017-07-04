<html>
<head>
    <style type="text/css"><!--
      body
      {
         font-family: Arial, sans-serif;
         font-size: 14px;
         color: #4c4c4c;
      }
      
      a, a:visited
      {
         color: #0072cf;
      }
      --></style>
</head>

<body bgcolor="#dddddd">
<table width="100%" cellpadding="20" cellspacing="0" border="0" bgcolor="#dddddd">
    <tr>
        <td width="100%" align="center">
            <table width="70%" cellpadding="0" cellspacing="0" bgcolor="white" style="background-color: white; border: 1px solid #aaaaaa;">
                <tr>
                    <td width="100%">
                        <table width="100%" cellpadding="0" cellspacing="0" border="0">
                            <tr>
                                <td style="padding: 10px 30px 0px;">
                                    <table width="100%" cellpadding="0" cellspacing="0" border="0">
                                        <tr>
                                            <td>
                                                <table cellpadding="0" cellspacing="0" border="0">
                                                    <tr>
                                                        <td>
                                                            <img src="${shareUrl}/res/components/images/task-64.png" alt="" width="64" height="64" border="0" style="padding-right: 20px;" />
                                                        </td>
                                                        <td>
                                                            <div style="font-size: 22px; padding-bottom: 4px;">
                                                                You have been assigned a task
                                                            </div>
                                                        </td>
                                                    </tr>
                                                </table>
                                                <div style="font-size: 14px; margin: 12px 0px 24px 0px; padding-top: 10px; border-top: 1px solid #aaaaaa;">
                                                    <p>Hi,</p>

                                                    <p>
                                                        You have been assigned the following task
                                                    </p>

                                                    <p><b>Task:</b> "${taskTitle}"</p>
                                                    <p><b>Workflow:</b> "${workflowTitle}"</p>
                                                    <p><b>Due date:</b> "${taskDueDate}"</p>

                                                <#if (workflowDescription)??><p>Workflow description: ${workflowDescription}</p></#if>

                                                <#if (workflowDocuments)??>
                                                    <table cellpadding="0" callspacing="0" border="0" bgcolor="#eeeeee" style="padding:10px; border: 1px solid #aaaaaa;">
                                                        <#list workflowDocuments as doc>
                                                            <tr>
                                                                <td>
                                                                    <table cellpadding="0" cellspacing="0" border="0">
                                                                        <tr>
                                                                            <td valign="top">
                                                                                <img src="${shareUrl}/res/components/images/generic-file.png" alt="" width="64" height="64" border="0" style="padding-right: 10px;" />
                                                                            </td>
                                                                            <td>
                                                                                <table cellpadding="2" cellspacing="0" border="0">
                                                                                    <tr>
                                                                                        <td><b>${doc.name}</b></td>
                                                                                    </tr>
                                                                                    <tr>
                                                                                        <td>Click <a href="${shareUrl}/proxy/alfresco/api/node/content/workspace/SpacesStore/${doc.id}/${doc.name}?a=true">here</a> to download the document:</td>
                                                                                    </tr>
                                                                                </table>
                                                                            </td>
                                                                        </tr>
                                                                    </table>
                                                                </td>
                                                            </tr>
                                                            <#if doc_has_next>
                                                                <tr><td><div style="border-top: 1px solid #aaaaaa; margin:12px;"></div></td></tr>
                                                            </#if>
                                                        </#list>
                                                    </table>
                                                </#if>

                                                    <p>Click <a href="${shareUrl}/page/task-edit?taskId=${taskId}">here</a> to edit the task</p>

                                                    <p>Sincerely,<br />
                                                        Alfresco powered by Alvex</p>
                                                </div>
                                            </td>
                                        </tr>
                                    </table>
                                </td>
                            </tr>
                        </table>
                    </td>
                </tr>
            </table>
        </td>
    </tr>
</table>
</body>
</html>