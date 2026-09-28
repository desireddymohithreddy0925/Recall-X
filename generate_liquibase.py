import yaml

tables = ["organization", "user", "system_app", "incident", "deployment", "engineering_experience", "architecture_decision", "troubleshooting_attempt", "lesson", "preventive_action", "pattern"]

changes = []
for table in tables:
    changes.append({
        "createTable": {
            "tableName": table,
            "columns": [
                {
                    "column": {
                        "name": "id",
                        "type": "bigint",
                        "autoIncrement": True,
                        "constraints": {"primaryKey": True, "nullable": False}
                    }
                },
                {
                    "column": {
                        "name": "created_at",
                        "type": "timestamp",
                        "defaultValueComputed": "CURRENT_TIMESTAMP"
                    }
                },
                {
                    "column": {
                        "name": "updated_at",
                        "type": "timestamp",
                        "defaultValueComputed": "CURRENT_TIMESTAMP"
                    }
                }
            ]
        }
    })

# Add specific columns to incident and troubleshooting_attempt
for change in changes:
    if change["createTable"]["tableName"] == "incident":
        change["createTable"]["columns"].append({"column": {"name": "title", "type": "varchar(255)", "constraints": {"nullable": False}}})
        change["createTable"]["columns"].append({"column": {"name": "description", "type": "text"}})
    elif change["createTable"]["tableName"] == "troubleshooting_attempt":
        change["createTable"]["columns"].append({"column": {"name": "action_taken", "type": "text"}})
        change["createTable"]["columns"].append({"column": {"name": "status", "type": "varchar(50)", "constraints": {"nullable": False}}})
        change["createTable"]["columns"].append({"column": {"name": "incident_id", "type": "bigint"}})
        
# Foreign key
changes.append({
    "addForeignKeyConstraint": {
        "baseTableName": "troubleshooting_attempt",
        "baseColumnNames": "incident_id",
        "constraintName": "fk_troubleshooting_incident",
        "referencedTableName": "incident",
        "referencedColumnNames": "id"
    }
})

data = {
    "databaseChangeLog": [
        {
            "changeSet": {
                "id": "2",
                "author": "recall-x",
                "changes": changes
            }
        }
    ]
}

with open("src/main/resources/db/changelog/changes/002-domain-model.yaml", "w") as f:
    yaml.dump(data, f, sort_keys=False)

print("Generated 002-domain-model.yaml")
