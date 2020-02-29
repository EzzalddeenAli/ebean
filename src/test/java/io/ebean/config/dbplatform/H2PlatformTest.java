package io.ebean.config.dbplatform;

import io.ebean.config.dbplatform.h2.H2Platform;
import io.ebeaninternal.dbmigration.ddlgeneration.platform.PlatformDdl;
import io.ebeaninternal.server.core.PlatformDdlBuilder;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class H2PlatformTest {

  @Test
  public void testTypeConversion() {

    PlatformDdl ddl = PlatformDdlBuilder.create(new H2Platform());

    assertThat(ddl.convert("clob")).isEqualTo("clob");
    assertThat(ddl.convert("json")).isEqualTo("clob");
    assertThat(ddl.convert("jsonb")).isEqualTo("clob");
    assertThat(ddl.convert("varchar(20)")).isEqualTo("varchar(20)");
    assertThat(ddl.convert("decimal(10)")).isEqualTo("decimal(10)");
    assertThat(ddl.convert("decimal(8,4)")).isEqualTo("decimal(8,4)");
    assertThat(ddl.convert("boolean")).isEqualTo("boolean");
    assertThat(ddl.convert("bit")).isEqualTo("bit");
  }

}
