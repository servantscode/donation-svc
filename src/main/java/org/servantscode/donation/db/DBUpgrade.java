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
                                           "check_number INTEGER, " +
                                           "transaction_id bigint, " +
                                           "batch_number INTEGER, " +
                                           "notes TEXT, " +
                                           "org_id INTEGER references organizations(id) ON DELETE CASCADE)");
        }

        if(!columnExists("donations", "batch_number")) {
            LOG.info("-- Adding column batch_number to donations");
            runSql("ALTER TABLE donations ADD COLUMN batch_number INTEGER");
        }

        if(!columnExists("donations", "notes")) {
            LOG.info("-- Adding column notes to donations");
            runSql("ALTER TABLE donations ADD COLUMN notes TEXT");
        }
    }
}
