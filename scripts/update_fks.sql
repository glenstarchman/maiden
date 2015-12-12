alter table project_award alter column submitted_by set default 9223372036854775806;
alter table project_link alter column submitted_by set default 9223372036854775806;
alter table project_invite alter column sent_by set default 9223372036854775806;
alter table project_media alter column added_by set default 9223372036854775806;
alter table project_oembed alter column submitted_by set default 9223372036854775806;
alter table team alter column added_by set default 9223372036854775806;
alter table team_member alter column added_by set default 9223372036854775806;
alter table taggable alter column actor_id set default 9223372036854775806;
