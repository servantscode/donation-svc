package org.servantscode.donation.db;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.servantscode.commons.db.AbstractDBUpgrade;

import java.sql.SQLException;

public class DBUpgrade extends AbstractDBUpgrade {
    private static final Logger LOG = LogManager.getLogger(DBUpgrade.class);

    @Override
    public void doUpgrade() throws SQLException {
        LOG.info("Verifying database structures.");

        if(!tableExists("funds")) {
            LOG.info("-- Creating funds table");
            runSql("CREATE TABLE funds (id SERIAL PRIMARY KEY, " +
                                       "name TEXT NOT NULL, " +
                                       "org_id INTEGER references organizations(id) ON DELETE CASCADE)");
            runSql("INSERT INTO funds (name, org_id) values ('general', 1)");
        }

        if(!tableExists("pledges")) {
            LOG.info("-- Creating pledges table");
            runSql("CREATE TABLE pledges (id SERIAL PRIMARY KEY, " +
                                         "family_id INTEGER REFERENCES families(id), " +
                                         "fund_id INTEGER REFERENCES funds(id) DEFAULT 1 NOT NULL, " +
                                         "pledge_type TEXT, " +
                                         "pledge_date DATE, " +
                                         "pledge_start DATE, " +
                                         "pledge_end DATE, " +
                                         "frequency TEXT, " +
                                         "pledge_increment FLOAT, " +
                                         "total_pledge FLOAT, " +
                                         "org_id INTEGER references organizations(id) ON DELETE CASCADE)");
        }

        if(!tableExists("donations")) {
            LOG.info("-- Creating donations table");
            runSql("CREATE TABLE donations (id BIGSERIAL PRIMARY KEY, " +
                                           "family_id INTEGER REFERENCES families(id), " +
                                           "fund_id INTEGER REFERENCES funds(id) DEFAULT 1 NOT NULL, " +
                                           "amount FLOAT, " +
                                           "date DATE, " +
                                           "type TEXT, " +
                                           "check_number BIGINT, " +
                                           "transaction_id TEXT, " +
                                           "batch_number INTEGER, " +
                                           "notes TEXT, " +
                                           "recorded_time TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(), " +
                                           "recorder_id INTEGER REFERENCES people(id) ON DELETE SET NULL, " +
                                           "deductible_amount FLOAT, " +
                                           "org_id INTEGER references organizations(id) ON DELETE CASCADE)");
        }

        if(!columnTypeMatches("donations", "transaction_id", "TEXT"))
            runSql("ALTER TABLE donations ALTER COLUMN transaction_id SET DATA TYPE TEXT");

        if(!columnExists("donations", "deductible_amount")) {
            ensureColumn("donations", "deductible_amount", "FLOAT");
            runSql("UPDATE donations SET deductible_amount=amount");
        }

        if(!columnTypeMatches("donations", "check_number", "BIGINT"))
            runSql("ALTER TABLE donations ALTER COLUMN check_number SET DATA TYPE BIGINT");

        if(!columnExists("donations", "pledge_id")) {
            ensureColumn("donations", "pledge_id", "INTEGER REFERENCES pledges(id) ON DELETE SET NULL");
            runSql("update donations d set pledge_id = (select id from pledges p where p.fund_id=d.fund_id AND d.family_id=p.family_id AND d.date >= p.pledge_start and d.date <= p.pledge_end limit 1)");
        }
    }
}

