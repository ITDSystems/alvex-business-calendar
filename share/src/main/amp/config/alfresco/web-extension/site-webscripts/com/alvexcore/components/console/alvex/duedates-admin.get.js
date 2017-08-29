var widgets = [
    {
        name: "alfresco/html/Spacer",
        config: {
            height: "10px"
        }
    }
];

var limits = eval('(' + remote.connect('alfresco').get('/api/alvex/duedates/limits') + ')').limits;

// W/A  for Aikau issue with non-editable integer fields
// See https://github.com/ITDSystems/alvex-business-calendar/issues/1

for each(var process in limits)
    for each(var limit in process.limits)
        limit.limit = limit.limit.toString();

//

for (var i in limits) {
    var process = limits[i];
    widgets.push({
        name: "alfresco/layout/Twister",
        config: {
            label: process.processLabel,
            headingLevel: 3,
            initiallyOpen: false,
            widgets: [
                {
                    name: "alfresco/lists/views/AlfListView",
                    config: {
                        currentData: {
                            items: process.limits
                        },
                        widgetsForHeader: [
                            {
                                name: "alfresco/lists/views/layouts/HeaderCell",
                                config: {
                                    label: msg.get("tool.duedates-admin.task")
                                }
                            },
                            {
                                name: "alfresco/lists/views/layouts/HeaderCell",
                                config: {
                                    label: msg.get("tool.duedates-admin.limit")
                                }
                            }
                        ],
                        widgets: [
                            {
                                name: "alfresco/lists/views/layouts/Row",
                                config: {
                                    widgets: [
                                        {
                                            name: "alfresco/lists/views/layouts/Cell",
                                            config: {
						additionalCssClasses: "mediumpad",
                                                widgets: [
                                                    {
                                                        name: "alfresco/renderers/Property",
                                                        config: {
                                                            propertyToRender: "taskLabel"
                                                        }
                                                    }
                                                ]
                                            }
                                        },
                                        {
					    name: "alfresco/lists/views/layouts/Cell",
					    config: {
						additionalCssClasses: "mediumpad",
						widgets: [{
                                                    name: "alfresco/renderers/InlineEditProperty",
                                                    config: {
                                                        propertyToRender: "limit",
                                                        refreshCurrentItem: true,
                                                        requirementConfig: {
                                                            initialValue: true
                                                        },
                                                        publishTopic: "ALF_CRUD_UPDATE",
                                                        publishPayloadType: "PROCESS",
                                                        publishPayloadModifiers: ["processCurrentItemTokens"],
                                                        publishPayloadItemMixin: false,
                                                        publishPayload: {
                                                            url: "api/alvex/duedates/limits/{limitKey}"
							}
						    }
                                                }]
                                            }
                                        }
                                    ]
                                }
                            }
                        ]
                    }
                }
            ]
        }
    });
}

model.jsonModel = {
    services: [
        {
            name: "alfresco/services/LoggingService",
            config: {
                loggingPreferences: {
                    enabled: true,
                    all: true,
                    warn: true,
                    error: true
                }
            }
        },
        "alfresco/services/CrudService"
    ],
    widgets: [{
        id: "SET_PAGE_TITLE",
        name: "alfresco/header/SetTitle",
        config: {
            title: msg.get("tool.duedates-admin.label")
        }
    }, {
        id: "SHARE_VERTICAL_LAYOUT",
        name: "alfresco/layout/VerticalWidgets",
        config: {
            widgets: widgets
        }
    }]
};
