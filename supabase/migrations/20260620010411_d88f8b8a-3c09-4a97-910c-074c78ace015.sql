REVOKE SELECT (password) ON public.bot_subagents FROM authenticated;
REVOKE SELECT (password) ON public.bot_subagents FROM anon;
REVOKE SELECT (password) ON public.bot_subagents FROM public;

DROP POLICY IF EXISTS "owner_manages_subagents" ON public.bot_subagents;

CREATE POLICY "owner_can_read_safe_subagent_fields"
ON public.bot_subagents
FOR SELECT
TO authenticated
USING (auth.uid() = owner_id);

CREATE POLICY "owner_can_insert_subagents"
ON public.bot_subagents
FOR INSERT
TO authenticated
WITH CHECK (auth.uid() = owner_id);

CREATE POLICY "owner_can_update_subagents"
ON public.bot_subagents
FOR UPDATE
TO authenticated
USING (auth.uid() = owner_id)
WITH CHECK (auth.uid() = owner_id);

CREATE POLICY "owner_can_delete_subagents"
ON public.bot_subagents
FOR DELETE
TO authenticated
USING (auth.uid() = owner_id);